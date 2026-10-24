$ErrorActionPreference = "Stop"

$repositoryRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$testExit = 1
Push-Location $repositoryRoot
try {
    & .\mvnw.cmd verify -Dgroups=Layer1
    $testExit = $LASTEXITCODE
}
finally {
    Pop-Location
}

if ($testExit -eq 0) {
    Write-Host "✅ Layer 1 integration tests PASSED"
}
else {
    Write-Host "❌ Layer 1 integration tests FAILED"
}
exit $testExit
