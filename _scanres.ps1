Set-Location 'C:\Users\wdsjx\Desktop\infinityminecraft-template-1.21.1'
Write-Output '=== all files under 模型资源 ==='
Get-ChildItem -Recurse -File -LiteralPath '资源文件\模型资源' -ErrorAction SilentlyContinue | ForEach-Object {
    ($_.LastWriteTime.ToString('MM-dd HH:mm')) + '  ' + $_.FullName.Substring((Get-Location).Path.Length + 1)
}
Write-Output ''
Write-Output '=== 资源文件: modified today OR name has eye-ish ==='
$today = (Get-Date).Date
Get-ChildItem -Recurse -File -LiteralPath '资源文件' -ErrorAction SilentlyContinue | Where-Object {
    $_.LastWriteTime -ge $today -or $_.Name -match '眼|邪|炽|eye|yan'
} | ForEach-Object {
    ($_.LastWriteTime.ToString('MM-dd HH:mm')) + '  ' + $_.FullName.Substring((Get-Location).Path.Length + 1)
}
