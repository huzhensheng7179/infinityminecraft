$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$path = 'C:\Users\wdsjx\Desktop\infinityminecraft-template-1.21.1\src\main\resources\assets\infinitycraft\textures\steampunk\steampunk_bow.png'

$bytes = [System.IO.File]::ReadAllBytes($path)
$ms = New-Object System.IO.MemoryStream(,$bytes)
$img = [System.Drawing.Image]::FromStream($ms)
Write-Output ("orig: " + $img.Width + "x" + $img.Height)

$size = 128
$bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($img, 0, 0, $size, $size)
$g.Dispose()
$img.Dispose()
$ms.Dispose()

$out = New-Object System.IO.MemoryStream
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
[System.IO.File]::WriteAllBytes($path, $out.ToArray())
$out.Dispose()
Write-Output ("saved 128x128 -> " + $path)
