$ErrorActionPreference = 'Continue'
$root = 'C:\Users\wdsjx\Desktop\infinityminecraft-template-1.21.1'
# 资源文件 = 8D44 6E90 6587 4EF6 ; 模型资源 = 6A21 578B 8D44 6E90
$ziyuan = [string][char]0x8D44 + [char]0x6E90 + [char]0x6587 + [char]0x4EF6
$moxing = [string][char]0x6A21 + [char]0x578B + [char]0x8D44 + [char]0x6E90
$modelDir = Join-Path (Join-Path $root $ziyuan) $moxing
Write-Output ("MODEL ROOT: " + $modelDir)
# find the "野火" subfolder (any directory under modelDir)
$wildfire = Get-ChildItem -LiteralPath $modelDir -Directory | Select-Object -First 1
Write-Output ("WILDFIRE FOLDER: " + $wildfire.FullName)
Get-ChildItem -LiteralPath $wildfire.FullName -Recurse -File | ForEach-Object {
    $rel = $_.FullName.Substring($wildfire.FullName.Length + 1)
    Write-Output ($rel + "  [" + $_.Length + " bytes]")
}
Write-Output "===== JSON FILES HEAD ====="
Get-ChildItem -LiteralPath $wildfire.FullName -Recurse -File -Filter *.json | ForEach-Object {
    Write-Output ("--- " + $_.Name + " [" + $_.Length + " bytes] ---")
    Get-Content -LiteralPath $_.FullName -TotalCount 3
}
Write-Output "===== MCMETA ====="
Get-ChildItem -LiteralPath $wildfire.FullName -Recurse -File -Filter *.mcmeta | ForEach-Object {
    Write-Output ("--- " + $_.Name + " ---")
    Get-Content -LiteralPath $_.FullName -Raw
}
