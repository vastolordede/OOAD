# Non-destructive environment preflight. Run from OOAD root: .\scripts\ooad-preflight.ps1
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Write-Host "OOAD root: $root"
$checks = @{
  'frontend/package.json' = Test-Path (Join-Path $root 'frontend/package.json')
  'frontend/package-lock.json' = Test-Path (Join-Path $root 'frontend/package-lock.json')
  'backend/build.gradle' = Test-Path (Join-Path $root 'backend/build.gradle')
  'backend/gradlew.bat' = Test-Path (Join-Path $root 'backend/gradlew.bat')
}
foreach ($name in $checks.Keys) {
  $state = if ($checks[$name]) {'OK'} else {'MISSING'}
  Write-Host "$state : $name"
}
Write-Host 'Git:'
git -C $root status -sb
if (-not $checks['frontend/package.json']) {
  throw 'Wrong repository checkout or missing frontend. Do not run npm until package.json exists.'
}
if (-not $checks['frontend/package-lock.json']) {
  Write-Warning 'npm ci requires package-lock.json. Inspect checkout; do not generate an unreviewed lockfile here.'
}
Write-Host 'Backend test DB: verify TEST_DB_URL + DEMO_TEST_DB_URL point to isolated databases containing TEST. No database has been changed.'
