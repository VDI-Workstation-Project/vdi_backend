param(
    [Parameter(Mandatory = $true)]
    [string]$UserId,
    [Parameter(Mandatory = $true)]
    [string]$FirstName,
    [Parameter(Mandatory = $true)]
    [string]$LastName,
    [Parameter(Mandatory = $true)]
    [string]$Password,
    [Parameter(Mandatory = $true)]
    [string]$PhoneNumber,
    [Parameter(Mandatory = $true)]
    [string]$Email,
    [Parameter(Mandatory = $true)]
    [string]$Title,
    [Parameter(Mandatory = $true)]
    [string]$ADServer,
    [Parameter(Mandatory = $true)]
    [string]$Username,
    [Parameter(Mandatory = $true)]
    [string]$AdminPassword,
    [Parameter(Mandatory = $true)]
    [string]$OUPath,
    [Parameter(Mandatory = $true)]
    [string]$SecurityGroupName  # VM별 보안 그룹 이름
)

Start-Sleep -Seconds 2

# 스크립트 위치 기준으로 로그 경로 설정
$ScriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$LogDir = Join-Path $ScriptPath "logs"
$LogPath = Join-Path $LogDir "ad_user_creation.log"
$ErrorLogPath = Join-Path $LogDir "ad_user_creation_error.log"

# 로그 디렉토리 생성 시도 (에러 처리 추가)
try
{
    if (!(Test-Path $LogDir))
    {
        Write-Host "로그 디렉토리 생성 시도: $LogDir"
        New-Item -ItemType Directory -Path $LogDir -Force -ErrorAction Stop
        Write-Host "로그 디렉토리 생성 성공: $LogDir"
    }
    else
    {
        Write-Host "기존 로그 디렉토리 사용: $LogDir"
    }

    # 로그 파일 생성 시도
    if (!(Test-Path $LogPath))
    {
        New-Item -ItemType File -Path $LogPath -Force -ErrorAction Stop
        "" | Out-File -FilePath $LogPath -Encoding UTF8
        Write-Host "로그 파일 생성됨: $LogPath"
    }
    if (!(Test-Path $ErrorLogPath))
    {
        New-Item -ItemType File -Path $ErrorLogPath -Force -ErrorAction Stop
        "" | Out-File -FilePath $ErrorLogPath -Encoding UTF8
        Write-Host "에러 로그 파일 생성됨: $ErrorLogPath"
    }

    # 로그 파일 쓰기 권한 테스트
    $testMessage = "Log initialization test: $( Get-Date )"
    $testMessage | Out-File -FilePath $LogPath -Append -Encoding UTF8
    $testMessage | Out-File -FilePath $ErrorLogPath -Append -Encoding UTF8
    Write-Host "로그 파일 쓰기 테스트 성공"
}
catch
{
    Write-Error "로그 설정 중 오류 발생: $( $_.Exception.Message )"
    Write-Error "스크립트 경로: $ScriptPath"
    Write-Error "로그 디렉토리 경로: $LogDir"
    throw
}

# 로그 함수 정의
function Write-Log
{
    param($Message)
    $LogMessage = "$( Get-Date -Format 'yyyy-MM-dd HH:mm:ss' ): $Message"
    Write-Host $LogMessage
    $LogMessage | Out-File -FilePath $LogPath -Append -Encoding UTF8
}

function Write-ErrorLog
{
    param($Message)
    $LogMessage = "$( Get-Date -Format 'yyyy-MM-dd HH:mm:ss' ): ERROR - $Message"
    Write-Error $LogMessage
    $LogMessage | Out-File -FilePath $ErrorLogPath -Append -Encoding UTF8
}


# 메인 스크립트 실행
try
{
    Write-Log "스크립트 시작 - 경로: $ScriptPath"
    Write-Log "로그 디렉토리: $LogDir"
    Write-Log "시작: AD 사용자 생성 - UserID: $UserId, Name: $FirstName $LastName"

    # VM에 원격 연결 설정
    Write-Log "원격 연결 설정 시도 - Server: $ADServer, Username: $Username"

    # 자격 증명 생성 시도
    try
    {
        $SecureAdminPassword = ConvertTo-SecureString $AdminPassword -AsPlainText -Force
        $Credential = New-Object System.Management.Automation.PSCredential ($Username, $SecureAdminPassword)
        Write-Log "자격 증명 생성 성공"
    }
    catch
    {
        Write-ErrorLog "자격 증명 생성 실패: $( $_.Exception.Message )"
        throw
    }

    # 원격 서버 연결 테스트
    Write-Log "원격 서버 연결 테스트 시작"
    $testConnection = Test-Connection -ComputerName $ADServer -Count 1 -Quiet
    if (-not $testConnection)
    {
        Write-ErrorLog "원격 서버 ($ADServer)에 연결할 수 없습니다."
        throw "원격 서버 연결 실패"
    }
    Write-Log "원격 서버 연결 테스트 성공"

    # 원격 세션 생성
    Write-Log "원격 세션 생성 시도"
    try
    {
        $Session = New-PSSession -ComputerName $ADServer -Credential $Credential -ErrorAction Stop
        if ($null -eq $Session)
        {
            throw "세션이 생성되지 않았습니다."
        }
        Write-Log "원격 세션 생성 성공 (SessionId: $( $Session.Id ))"
    }
    catch
    {
        Write-ErrorLog "원격 세션 생성 실패: $( $_.Exception.Message )"
        if ($_.Exception.Message -like "*액세스가 거부되었습니다*")
        {
            Write-ErrorLog "자격 증명이 올바르지 않거나 권한이 부족합니다."
        }
        throw
    }

    # 세션 상태 확인
    if ($Session.State -ne "Opened")
    {
        Write-ErrorLog "세션이 올바른 상태가 아닙니다. 현재 상태: $( $Session.State )"
        throw "잘못된 세션 상태"
    }

    Write-Log "세션 상태 확인 완료"

    # 원격 서버에서 AD 명령 실행
    Write-Log "AD 사용자 생성 명령 실행 시작"

    $scriptBlock = {
        param($UserId, $FirstName, $LastName, $Password, $PhoneNumber, $Email, $Title, $SecurityGroupName, $OUPath)

        # 보안 그룹 생성 함수
        function Create-VMSecurityGroup
        {
            param($GroupName, $OUPath)

            try
            {
                if (-not (Get-ADGroup -Filter { Name -eq $GroupName } -ErrorAction SilentlyContinue))
                {
                    New-ADGroup -Name $GroupName `
                           -GroupScope DomainLocal `
                           -GroupCategory Security `
                           -Path $OUPath `
                           -Description "Security group for VM access control created at $( Get-Date )"
                    Write-Host "보안 그룹 생성 완료: $GroupName"
                }
                else
                {
                    Write-Host "보안 그룹이 이미 존재함: $GroupName"
                }
            }
            catch
            {
                throw "보안 그룹 생성 실패: $( $_.Exception.Message )"
            }
        }

        Write-Host "Active Directory 모듈 임포트"
        Import-Module ActiveDirectory

        # OU 존재 여부 확인
        Write-Host "OU 확인 중: $OUPath"
        if (-not [adsi]::Exists("LDAP://$OUPath")) {
            throw "지정된 OU가 존재하지 않습니다: $OUPath"
        }
        Write-Host "OU 확인 완료: $OUPath"

        # 보안 그룹 생성
        Create-VMSecurityGroup -GroupName $SecurityGroupName -OUPath $OUPath
        Write-Host "보안 그룹 생성 완료: $SecurityGroupName"

        Write-Host "사용자 계정 생성 시도"
        $SecurePassword = ConvertTo-SecureString $Password -AsPlainText -Force

        try {
            New-ADUser `
            -SamAccountName $UserId `
            -UserPrincipalName $Email `
            -Name "$FirstName $LastName" `
            -GivenName $FirstName `
            -Surname $LastName `
            -EmailAddress $Email `
            -MobilePhone $PhoneNumber `
            -Title $Title `
            -Enabled $True `
            -ChangePasswordAtLogon $True `
            -AccountPassword $SecurePassword `
            -Path $OUPath

            Write-Host "기본 사용자 계정 생성 완료"

            # 사용자를 보안 그룹에 추가
            Add-ADGroupMember -Identity $SecurityGroupName -Members $UserId
            Write-Host "보안 그룹 할당 완료: $SecurityGroupName"

            # Domain Users 그룹에 추가
            Add-ADGroupMember -Identity "Domain Users" -Members $UserId
            Write-Host "Domain Users 그룹 추가 완료"

        } catch {
            Write-Error "사용자 생성 또는 그룹 할당 실패: $($_.Exception.Message)"
            throw
        }

        비밀번호 변경 강제 설정 (여기 추가)
        Set-ADUser -Identity $UserId -ChangePasswordAtLogon $true

        Write-Host "비밀번호 변경 강제 설정 완료"

        Write-Host "추가 속성 설정 시작"
        Set-ADUser -Identity $UserId -Replace @{
            telephoneNumber = $PhoneNumber
            mail = $Email
        }
        Write-Host "추가 속성 설정 완료"

        Write-Host "Domain Users 그룹에 사용자 추가 시도"
        Add-ADGroupMember -Identity "Domain Users" -Members $UserId
        Write-Host "그룹 추가 완료"
    }

    Invoke-Command -Session $Session -ScriptBlock $scriptBlock -ArgumentList $UserId, $FirstName, $LastName, $Password, $PhoneNumber, $Email, $Title, $SecurityGroupName, $OUPath

    Remove-PSSession $Session
    Write-Log "원격 세션 종료"

    Write-Log "사용자 생성 완료: $UserId"
    Write-Host "User successfully created in AD"
    exit 0
}
catch
{
    $errorMessage = $_.Exception.Message
    $errorDetail = $_.Exception.StackTrace
    Write-ErrorLog "오류 발생: $errorMessage"
    Write-ErrorLog "상세 오류: $errorDetail"
    Write-ErrorLog "스크립트 실행 위치: $( $_.InvocationInfo.PositionMessage )"
    exit 1
}