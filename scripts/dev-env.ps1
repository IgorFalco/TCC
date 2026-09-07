# Carrega o ambiente de desenvolvimento nesta sessão do PowerShell:
#   . .\scripts\dev-env.ps1
# Fixa o JAVA_HOME no JDK 21 (a máquina também tem o JDK 25).

$jdk21 = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
if (Test-Path $jdk21) {
    $env:JAVA_HOME = $jdk21
    $env:Path = "$jdk21\bin;" + ($env:Path -split ';' | Where-Object { $_ -notlike '*Java*' -and $_ -ne '' } | Select-Object -Unique) -join ';'
    Write-Host "JAVA_HOME = $env:JAVA_HOME"
    & "$jdk21\bin\java.exe" -version
} else {
    Write-Warning "JDK 21 não encontrado em $jdk21"
}
