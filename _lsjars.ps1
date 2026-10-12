$ErrorActionPreference = 'SilentlyContinue'
$base = Join-Path $env:USERPROFILE '.gradle'
Write-Output "BASE=$base"
Get-ChildItem $base -Recurse -Filter '*.jar' |
    Where-Object { $_.Length -gt 2000000 } |
    ForEach-Object { $_.FullName } |
    Where-Object { $_ -match 'sources|client-extra|neoforge|neoform' } |
    Select-Object -First 60
