param(
    [Parameter(Mandatory=$true)]
    [string]$VmPath,
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

# 상세한 오류 정보 활성화
$ErrorActionPreference = "Stop"
$VerbosePreference = "Continue"

try {
    Write-Verbose "스크립트 시작 - 매개변수 확인"
    Write-Verbose "VmPath: $VmPath"
    Write-Verbose "UserId: $UserId"
    Write-Verbose "OUPath: $OUPath"
    Write-Verbose "SecurityGroupName: $SecurityGroupName"
    Write-Verbose "ADServer: $ADServer"
    Write-Verbose "VmName: $VmName"

    # 자격 증명 생성
    Write-Verbose "관리자 자격 증명 생성 중..."
    $SecurePassword = ConvertTo-SecureString $AdminPassword -AsPlainText -Force
    $Credential = New-Object System.Management.Automation.PSCredential($AdminUsername, $SecurePassword)

    # 원격 서버 연결 테스트
    Write-Verbose "원격 서버 연결 테스트 시작"
    if (-not (Test-Connection -ComputerName $ADServer -Count 1 -Quiet)) {
        throw "원격 서버 ($ADServer)에 연결할 수 없습니다."
    }

    # 원격 세션 생성 및 실행
    Write-Verbose "원격 세션 생성 시도"
    $Session = New-PSSession -ComputerName $ADServer -Credential $Credential

    $result = Invoke-Command -Session $Session -ScriptBlock {
        param($VmName, $OUPath, $SecurityGroupName, $Credential)

        Import-Module ActiveDirectory

        try {
            # 기존 컴퓨터 계정 확인 및 제거
            $computerExists = Get-ADComputer -Filter {Name -eq $VmName} -ErrorAction SilentlyContinue
            if ($computerExists) {
                Write-Verbose "기존 컴퓨터 계정 제거 중..."
                Remove-ADComputer -Identity $VmName -Confirm:$false -ErrorAction Stop
            }

            Write-Verbose "새 컴퓨터 계정 생성 중..."
            New-ADComputer -Name $VmName `
                          -Path $OUPath `
                          -Enabled $true `
                          -Credential $Credential

            Write-Verbose "보안 그룹 멤버 추가 중..."
            Add-ADGroupMember -Identity $SecurityGroupName `
                            -Members "$VmName$" `
                            -Credential $Credential

            Write-Host "VM 보안 설정 완료"
            return $true
        }
        catch {
            Write-Error "원격 실행 중 오류: $($_.Exception.Message)"
            Write-Error "스택 트레이스: $($_.ScriptStackTrace)"
            return $false
        }
    } -ArgumentList $VmName, $OUPath, $SecurityGroupName, $Credential

    Remove-PSSession $Session

    if (-not $result) {
        throw "원격 실행 실패"
    }

    exit 0
}
catch {
    Write-Error "VM 보안 설정 실패: $($_.Exception.Message)"
    Write-Error "스택 트레이스: $($_.ScriptStackTrace)"
    exit 1
}