$src = "C:\Users\konra\OneDrive\Pulpit\vipshop\Custom"
$dst = "C:\Users\konra\AppData\Roaming\.minecraft\resourcepacks\Custom"

Write-Host "Auto-sync ON - watching for changes... (Ctrl+C to stop)" -ForegroundColor Green

$fsw = New-Object IO.FileSystemWatcher $src, "*.*" -Property @{
    IncludeSubdirectories = $true
    NotifyFilter = [IO.NotifyFilters]::LastWrite
}

while ($true) {
    $result = $fsw.WaitForChanged([IO.WatcherChangeTypes]::All, 1000)
    if (-not $result.TimedOut) {
        robocopy $src $dst /E /R:1 /W:1 /NFL /NDL /NJH /NJS | Out-Null
        $ts = Get-Date -Format "HH:mm:ss"
        Write-Host "[$ts] Synced: $($result.Name)" -ForegroundColor Cyan
    }
}
