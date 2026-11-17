$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot '.env.local'
if (-not (Test-Path -LiteralPath $envFile)) {
    Write-Error 'Falta backend/.env.local. Copia .env.example a .env.local y configura la contraseña de PostgreSQL.'
    exit 1
}

Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
        $parts = $line.Split('=', 2)
        [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), 'Process')
    }
}

$jbr = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'
if (Test-Path (Join-Path $jbr 'bin\java.exe')) {
    $env:JAVA_HOME = $jbr
    $env:PATH = (Join-Path $jbr 'bin') + ';' + $env:PATH
}

Push-Location -LiteralPath $PSScriptRoot
try {
    & .\gradlew.bat bootRun
    $gradleExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $gradleExitCode
