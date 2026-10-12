$procs = Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'forgeclientdev|infinitycraft|runClient' }
if ($procs) {
    foreach ($p in $procs) {
        Write-Output ("killing PID " + $p.ProcessId)
        Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
    }
} else {
    Write-Output "no matching java client process found"
}
