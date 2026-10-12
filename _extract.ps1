$ErrorActionPreference = 'SilentlyContinue'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$roots = @("C:\Users\wdsjx\.gradle\caches")
$candidates = @()
foreach ($r in $roots) {
    $jars = Get-ChildItem -Recurse -Path $r -Include '*.jar' -ErrorAction SilentlyContinue
    foreach ($j in $jars) {
        if ($j.Name -match 'sources' -and $j.Name -match 'minecraft|client|neoform|joined|merged') {
            $candidates += $j.FullName
        }
    }
}
Write-Output "=== candidate MC source jars ==="
$candidates | ForEach-Object { Write-Output $_ }

foreach ($jar in $candidates) {
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
        $entry = $zip.Entries | Where-Object { $_.FullName -like '*client/model/geom/ModelPart.java' } | Select-Object -First 1
        if ($entry) {
            Write-Output "=== FOUND ModelPart.java in $jar ==="
            $sr = New-Object System.IO.StreamReader($entry.Open())
            $content = $sr.ReadToEnd()
            $sr.Close()
            # print render method region
            $lines = $content -split "`n"
            for ($i=0; $i -lt $lines.Count; $i++) {
                if ($lines[$i] -match 'public void render\(|void translateAndRotate|private void render|compile\(') {
                    $start = $i
                    $end = [Math]::Min($i+18, $lines.Count-1)
                    for ($k=$start; $k -le $end; $k++) { Write-Output $lines[$k] }
                    Write-Output '----'
                }
            }
            $zip.Dispose()
            break
        }
        $zip.Dispose()
    } catch {}
}
