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

try {
    Write-Verbose "Script started - Checking parameters"
    Write-Verbose "VmxPath: $VmxPath"
    Write-Verbose "UserId: $UserId"
    Write-Verbose "OUPath: $OUPath"
    Write-Verbose "SecurityGroupName: $SecurityGroupName"
    Write-Verbose "VmName: $VmName"

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
            Write-Host "VM AD registration completed"
            return $group.SID.Value
        }
        catch {
            Write-Error "Error during AD operations: $($_.Exception.Message)"
            throw
        }
    } -ArgumentList $VmName, $OUPath, $SecurityGroupName

    Remove-PSSession $ADSession
    exit 0
}
catch {
    Write-Error "VM AD registration failed: $($_.Exception.Message)"
    Write-Error "Error location: $($_.InvocationInfo.PositionMessage)"
    Write-Error "Stack trace: $($_.ScriptStackTrace)"
    exit 1
}