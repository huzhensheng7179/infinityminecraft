Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = "C:\Users\wdsjx\.gradle\caches\neoformruntime\intermediate_results\sourcesAndCompiledWithNeoForge_d2f97a8653510b5d9cea5b9705b2464bffee550b_output.jar"
if (-not (Test-Path $jar)) {
  $jar = Get-ChildItem "C:\Users\wdsjx\.gradle\caches\neoformruntime\intermediate_results\" -Filter "sourcesAndCompiledWithNeoForge_*_output.jar" | Select-Object -First 1 -ExpandProperty FullName
}
Write-Output "USING=$jar"
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
function Dump($entryName, $outName) {
  $e = $zip.Entries | Where-Object { $_.FullName -eq $entryName } | Select-Object -First 1
  if ($e) {
    $sr = New-Object System.IO.StreamReader($e.Open())
    $txt = $sr.ReadToEnd(); $sr.Close()
    $dest = Join-Path "C:\Users\wdsjx\Desktop\infinityminecraft-template-1.21.1" $outName
    Set-Content -Path $dest -Value $txt -Encoding UTF8
    Write-Output ("OK " + $entryName + " -> " + $outName + " (" + $txt.Length + " chars)")
  } else {
    Write-Output ("MISS " + $entryName)
  }
}
Dump "net/minecraft/world/item/SwordItem.java" "_SwordItem_src.java"
Dump "net/minecraft/world/item/Item.java" "_Item_src.java"
Dump "net/minecraft/world/item/ItemStack.java" "_ItemStack_src.java"
$zip.Dispose()
