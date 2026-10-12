$ErrorActionPreference = 'Stop'
$proj = 'C:\Users\wdsjx\Desktop\infinityminecraft-template-1.21.1'
$ziyuan = [string][char]0x8D44 + [char]0x6E90 + [char]0x6587 + [char]0x4EF6
$moxing = [string][char]0x6A21 + [char]0x578B + [char]0x8D44 + [char]0x6E90
$modelRoot = Join-Path (Join-Path $proj $ziyuan) $moxing
$wildfire = Get-ChildItem -LiteralPath $modelRoot -Directory | Select-Object -First 1
$src = $wildfire.FullName
Write-Output ("SRC: " + $src)

$assetsBase = Join-Path $proj 'src\main\resources\assets\infinitycraft'
$texDir = Join-Path $assetsBase 'textures\steampunk'
$modelDir = Join-Path $assetsBase 'models\item'
New-Item -ItemType Directory -Force -Path $texDir | Out-Null
New-Item -ItemType Directory -Force -Path $modelDir | Out-Null

function CopyTo($fileName, $destDir) {
    $s = Join-Path $src $fileName
    if (Test-Path -LiteralPath $s) {
        Copy-Item -LiteralPath $s -Destination (Join-Path $destDir $fileName) -Force
        Write-Output ("OK  " + $fileName)
    } else {
        Write-Output ("MISS " + $fileName)
    }
}
CopyTo 'steampunk_bow.png' $texDir
CopyTo 'steampunk_bowstring.png' $texDir
CopyTo 'steampunk_gear.png' $texDir
CopyTo 'steampunk_gear.png.mcmeta' $texDir
CopyTo 'steam.png' $texDir
CopyTo 'steam.png.mcmeta' $texDir

# model json -> models/item/wildfire_bow.json
$modelSrc = Join-Path $src 'steampunk_bow_0.json'
Copy-Item -LiteralPath $modelSrc -Destination (Join-Path $modelDir 'wildfire_bow.json') -Force
Write-Output "OK  model -> wildfire_bow.json"
Write-Output "===== texture dir now ====="
Get-ChildItem -LiteralPath $texDir | ForEach-Object { $_.Name + " [" + $_.Length + "]" }
