Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = "C:\Users\wdsjx\.gradle\caches\neoformruntime\intermediate_results\sourcesAndCompiledWithNeoForge_d2f97a8653510b5d9cea5b9705b2464bffee550b_output.jar"
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)

Write-Output "== base classes present =="
foreach ($e in $zip.Entries) {
  if ($e.FullName -match "client/particles/(SpriteSheetParticle|TextureSheetParticle|ParticleProvider|SpriteSetAware)\.java$") { Write-Output $e.FullName }
}
Write-Output "== TextureSheetParticle / SpriteSheetParticle ctor =="
foreach ($e in $zip.Entries) {
  if ($e.FullName -match "client/particles/(TextureSheetParticle|SpriteSheetParticle)\.java$") {
    $sr = New-Object System.IO.StreamReader($e.Open()); $lines = ($sr.ReadToEnd()) -split "`n"; $sr.Close()
    for ($i=0; $i -lt 40 -and $i -lt $lines.Count; $i++) {
      if ($lines[$i] -match "class .*Particle|Particle\(") { Write-Output ($e.Name+":"+(($i+1))+": "+$lines[$i].Trim()) }
    }
  }
}
Write-Output "== ParticleProvider.java full =="
foreach ($e in $zip.Entries) {
  if ($e.FullName -match "client/particles/ParticleProvider\.java$") {
    $sr = New-Object System.IO.StreamReader($e.Open()); Write-Output $sr.ReadToEnd(); $sr.Close()
  }
}
Write-Output "== SpriteSetAware.java full =="
foreach ($e in $zip.Entries) {
  if ($e.FullName -match "client/particles/SpriteSetAware\.java$") {
    $sr = New-Object System.IO.StreamReader($e.Open()); Write-Output $sr.ReadToEnd(); $sr.Close()
  }
}
$zip.Dispose()
