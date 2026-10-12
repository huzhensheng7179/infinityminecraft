$ErrorActionPreference = 'SilentlyContinue'
$roots = @("$HOME\.gradle\caches", "C:\Users\wdsjx\.gradle\caches")
$found = @()
foreach ($r in $roots) {
    if (Test-Path $r) {
        $jars = Get-ChildItem -Recurse -Path $r -Include '*.jar' -ErrorAction SilentlyContinue
        foreach ($j in $jars) {
            if ($j.Name -match 'sources|Sources') {
                $found += $j.FullName
            }
        }
    }
}
Write-Output "=== source jars found (first 15) ==="
$found | Select-Object -First 15 | ForEach-Object { Write-Output $_ }
