param(
    [Parameter(Mandatory=$true)]
    [string]$VmxPath,
    [Parameter(Mandatory=$true)]
    [string]$AdminUsername,
    [Parameter(Mandatory=$true)]
    [string]$AdminPassword,
    [Parameter(Mandatory=$true)]
    [string]$SecurityGroupName,
    [Parameter(Mandatory=$true)]
    [string]$VmRunPath
)

$ErrorActionPreference = "Stop"
$VerbosePreference = "Continue"

function Wait-VMToolsReady {
    param (
        [string]$VmxPath,
        [string]$VmRunPath,
        [int]$TimeoutSeconds = 180
    )

    Write-Verbose "Waiting for VMware Tools to be ready..."
    $startTime = Get-Date

    while ($true) {
        $toolsStatus = & $VmRunPath -T ws checkToolsState $VmxPath 2>&1

        if ($toolsStatus -like "*running*") {
            Write-Verbose "VMware Tools is running"
            return $true
        }

        if (((Get-Date) - $startTime).TotalSeconds -gt $TimeoutSeconds) {
            throw "Timeout waiting for VMware Tools to be ready"
        }

        Start-Sleep -Seconds 5
    }
}

try {
    # VM 시작 전 상태 확인
    $vmState = & $VmRunPath -T ws list | Select-String -Pattern $VmxPath
    if ($vmState) {
        Write-Verbose "VM is already running. Attempting to stop..."
        & $VmRunPath -T ws stop $VmxPath soft
        Start-Sleep -Seconds 10
    }

    # VM 시작 시도
    Write-Verbose "Starting VM..."
    $startResult = &$VmRunPath -T ws start $VmxPath nogui 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to start VM: $startResult"
    }

    # VMware Tools 상태 확인 및 대기
    if (-not (Wait-VMToolsReady -VmxPath $VmxPath -VmRunPath $VmRunPath)) {
        throw "Failed to detect running VMware Tools"
    }

    # Tools가 실행된 후 추가 대기 시간
    Write-Verbose "Waiting additional time for system services..."
    Start-Sleep -Seconds 30

    # VM에서 보안 설정 스크립트 실행
    Write-Verbose "Running security settings script..."

    #    # 스크립트 파일 존재 여부 확인 부분 수정
    #    Write-Verbose "Checking if script file exists..."
    #    $checkScript = & $VmRunPath -T ws -gu $AdminUsername -gp $AdminPassword runProgramInGuest $VmxPath C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
    #    "-Command `"Write-Host (Test-Path 'C:\Scripts\security_settings.ps1'); exit (if(Test-Path 'C:\Scripts\security_settings.ps1') { 0 } else { 1 })`"" 2>&1
    #
    #    Write-Verbose "Check script result: $checkScript"
    #
    #    if ($LASTEXITCODE -ne 0) {
    #        # 디버깅을 위한 추가 정보 수집
    #        Write-Verbose "Checking Scripts directory..."
    #        $dirCheck = & $VmRunPath -T ws -gu $AdminUsername -gp $AdminPassword runProgramInGuest $VmxPath C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
    #        "-Command `"Get-ChildItem 'C:\Scripts' | Select-Object Name, Length, LastWriteTime | Format-List`"" 2>&1
    #
    #        Write-Verbose "Directory contents: $dirCheck"
    #        throw "Security settings script not found. Exit code: $LASTEXITCODE, Output: $checkScript"
    #    }

    #    # 스크립트 실행 권한 확인
    #    Write-Verbose "Checking script execution policy..."
    #    $policyResult = & $VmRunPath -T ws -gu $AdminUsername -gp $AdminPassword runProgramInGuest $VmxPath C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
    #        "-Command `"Get-ExecutionPolicy`"" 2>&1
    #
    #    Write-Verbose "Current execution policy: $policyResult"

    # 실행 정책 변경
#    Write-Verbose "Setting ExecutionPolicy to Unrestricted..."
#
#
#    $unRestrictPolicyResult = & $VmRunPath -T ws -gu $AdminUsername -gp $AdminPassword runProgramInGuest $VmxPath C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -Command
#    "Set-ExecutionPolicy Unrestricted -Scope Process -Force" 2>&1
#    # 실행 결과를 출력
#    Write-Verbose $unRestrictPolicyResult
#    # 실행 정책 변경
#    Write-Verbose "Setting ExecutionPolicy to Unrestricted..."
#
#    $unRestrictPolicyResult = & $VmRunPath -T ws -gu $AdminUsername -gp $AdminPassword runProgramInGuest $VmxPath C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -Command
#    "Set-ExecutionPolicy Unrestricted -Scope Process -Force" 2>&1
#    # 실행 결과를 출력
#    Write-Verbose $unRestrictPolicyResult
#    # 실행 정책 변경
#    Write-Verbose "Setting ExecutionPolicy to Unrestricted..."
#
#    $unRestrictPolicyResult = & $VmRunPath -T ws -gu $AdminUsername -gp $AdminPassword runProgramInGuest $VmxPath C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -Command
#    "Set-ExecutionPolicy Unrestricted -Scope Process -Force" 2>&1
#    # 실행 결과를 출력
#    Write-Verbose $unRestrictPolicyResult
#
#
#    # 결과 확인 로그
#    Write-Verbose "ExecutionPolicy change result: $unRestrictPolicyResult"

    # 보안 설정 스크립트 실행
    Write-Verbose "Executing security settings script..."

    $scriptResult = & $VmRunPath -T ws -gu $AdminUsername -gp $AdminPassword `
    runProgramInGuest $VmxPath `
    C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe `
    "Set-ExecutionPolicy"` Bypass `
    C:\Scripts\security_settings.ps1 -SecurityGroupName $SecurityGroupName -Verbose` 2>&1

    Write-Verbose "Script execution result: $scriptResult"

    if ($LASTEXITCODE -ne 0) {
        throw "Security settings script failed with exit code $LASTEXITCODE : $scriptResult"
    }

    # 스크립트 실행 결과 확인
    Write-Verbose "Verifying security settings..."
    $verifyResult = & $VmRunPath -T ws -gu $AdminUsername -gp $AdminPassword runProgramInGuest $VmxPath C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
    "-Command `"if (!(Get-LocalGroup -Name '$SecurityGroupName' -ErrorAction SilentlyContinue)) { exit 1 }`"" 2>&1

    if ($LASTEXITCODE -ne 0) {
        throw "Security settings verification failed: Security group not found"
    }

    # 스크립트 파일 정리
    Write-Verbose "Cleaning up script files..."
    $cleanupResult = & $VmRunPath -T ws -gu $AdminUsername -gp $AdminPassword runPrVmxPath C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
    "-Command `"Remove-Item -Path 'C:\Scripts\security_settings.ps1' -Force; Remove-Item -Path 'C:\Scripts' -Force`"" 2>&1

    if ($LASTEXITCODE -ne 0) {
        throw "Cleanup failed: $cleanupResult"
    }

    # VM 종료
    Write-Verbose "VM shutdown in progress..."
    & $VmRunPath -T ws stop $VmxPath soft
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to shutdown VM"
    }

    Write-Host "VM security settings completed and cleanup finished."
    exit 0
}
catch {
    Write-Error "Error during VM security settings: $($_.Exception.Message)"
    Write-Error "Stack Trace: $($_.ScriptStackTrace)"

    try {
        & $VmRunPath -T ws stop $VmxPath hard
    } catch {
        Write-Error "Force shutdown of VM failed"
    }

    exit 1
}