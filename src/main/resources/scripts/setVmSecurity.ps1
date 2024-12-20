param(
    [Parameter(Mandatory=$true)]
    [string]$VmxPath,
    [Parameter(Mandatory=$true)]
    [string]$UserId,
    [Parameter(Mandatory=$true)]
    [string]$AdminUsername,
    [Parameter(Mandatory=$true)]
    [string]$AdminPassword,
    [Parameter(Mandatory=$true)]
    [string]$OUPath,
    [Parameter(Mandatory=$true)]
    [string]$SecurityGroupName,
    [Parameter(Mandatory=$true)]
    [string]$ADServer,
    [Parameter(Mandatory=$true)]
    [string]$VmName
)

$ErrorActionPreference = "Stop"
$VerbosePreference = "Continue"

function Start-VMInBackground {
   param([string]$VmxPath)
    Write-Verbose "Starting VM in headless mode: $VmxPath"
   # 새로운 방식으로 시도
   Start-Process -FilePath 'C:\Program Files (x86)\VMware\VMware Player\vmplayer.exe' `
                -ArgumentList "-q", "-X", "`"$VmxPath`"" `
                -WindowStyle Hidden `
                -NoNewWindow
   
   # VM 부팅 대기 시간
   Write-Verbose "Waiting for VM to boot..."
   Start-Sleep -Seconds 30

function Wait-ForDomainJoin {
    param (
        [string]$ComputerName,
        [int]$MaxAttempts = 6,
        [int]$DelaySeconds = 5
    )

    for ($i = 1; $i -le $MaxAttempts; $i++) {
        Write-Verbose "Checking domain join status attempt $i/$MaxAttempts"
        try {
            $result = Test-ComputerSecureChannel -Server $ComputerName
            if ($result) {
                Write-Verbose "Domain join confirmed"
                return $true
            }
        }
        catch {
            Write-Verbose "Domain join still in progress..."
        }
        if ($i -lt $MaxAttempts) {
            Start-Sleep -Seconds $DelaySeconds
        }
    }
    throw "Domain join verification timeout"
}

try {
    Write-Verbose "Script started - Checking parameters"
    Write-Verbose "VmxPath: $VmxPath"
    Write-Verbose "UserId: $UserId"
    Write-Verbose "OUPath: $OUPath"
    Write-Verbose "SecurityGroupName: $SecurityGroupName"
    Write-Verbose "VmName: $VmName"

    # VM 시작
    Start-VMInBackground -VmxPath $VmxPath

    $SecurePassword = ConvertTo-SecureString $AdminPassword -AsPlainText -Force
    $Credential = New-Object System.Management.Automation.PSCredential($AdminUsername, $SecurePassword)

    if (-not (Test-Connection -ComputerName $ADServer -Count 1 -Quiet)) {
        throw "Cannot connect to AD server ($ADServer)"
    }

    $ADSession = New-PSSession -ComputerName $ADServer -Credential $Credential

    $groupSID = Invoke-Command -Session $ADSession -ScriptBlock {
        param($VmName, $OUPath, $SecurityGroupName)

        Import-Module ActiveDirectory

        try {
            $computerExists = Get-ADComputer -Filter {Name -eq $VmName} -ErrorAction SilentlyContinue
            if ($computerExists) {
                Remove-ADComputer -Identity $VmName -Confirm:$false -ErrorAction Stop
            }

            New-ADComputer -Name $VmName -Path $OUPath -Enabled $true
            Add-ADGroupMember -Identity $SecurityGroupName -Members "$VmName$"

            $group = Get-ADGroup -Identity $SecurityGroupName
            return $group.SID.Value
        }
        catch {
            Write-Error "Error during AD operations: $($_.Exception.Message)"
            throw
        }
    } -ArgumentList $VmName, $OUPath, $SecurityGroupName

    Remove-PSSession $ADSession

    Wait-ForDomainJoin -ComputerName $VmName

    Write-Verbose "Attempting to connect to VM: $VmName"
    $VMSession = New-PSSession -ComputerName $VmName -Credential $Credential

    $result = Invoke-Command -Session $VMSession -ScriptBlock {
        param($SecurityGroupSID)

        try {
            $tempPath = "$env:TEMP\secpol.inf"
            $secDB = "$env:TEMP\secpol.sdb"

            secedit /export /cfg $tempPath

            $content = Get-Content $tempPath
            $content = $content -replace "SeInteractiveLogonRight = (.*)", "SeInteractiveLogonRight = *S-1-5-32-544,$SecurityGroupSID"
            $content = $content -replace "SeNetworkLogonRight = (.*)", "SeNetworkLogonRight = *S-1-5-32-544,$SecurityGroupSID"
            $content | Set-Content $tempPath -Force

            secedit /configure /db $secDB /cfg $tempPath /areas USER_RIGHTS
            gpupdate /force

            Remove-Item $tempPath -Force -ErrorAction SilentlyContinue
            Remove-Item $secDB -Force -ErrorAction SilentlyContinue

            Write-Host "VM security configuration completed"
            return $true
        }
        catch {
            Write-Error "Error setting security policy: $($_.Exception.Message)"
            return $false
        }
    } -ArgumentList $groupSID

    Remove-PSSession $VMSession

    if (-not $result) {
        throw "VM security policy configuration failed"
    }

    # VM 종료
    & 'C:\Program Files (x86)\VMware\VMware Workstation\vmplayer.exe' -q $VmxPath

    exit 0
}
catch {
    Write-Error "VM security setup failed: $($_.Exception.Message)"
    Write-Error "Error location: $($_.InvocationInfo.PositionMessage)"
    Write-Error "Stack trace: $($_.ScriptStackTrace)"

    # 오류 발생 시 VM 강제 종료 시도
    if ($VmxPath) {
        & 'C:\Program Files (x86)\VMware\VMware Workstation\vmplayer.exe' -q $VmxPath
    }

    exit 1
}