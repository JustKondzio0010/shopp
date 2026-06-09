$base = "C:\Users\konra\AppData\Roaming\.minecraft\resourcepacks\Custom\assets"
$namespaces = Get-ChildItem -Path $base -Directory

foreach ($ns in $namespaces) {
    $texDir = Join-Path $ns.FullName "textures"
    if (Test-Path $texDir) {
        $atlasDir = Join-Path $ns.FullName "atlases"
        if (-not (Test-Path $atlasDir)) {
            New-Item -ItemType Directory -Force -Path $atlasDir | Out-Null
        }
        
        $dirs = Get-ChildItem -Path $texDir -Directory -Recurse
        $sources = @()
        
        # Add root textures of this namespace if there are any files
        $rootFiles = Get-ChildItem -Path $texDir -File -Filter "*.png"
        if ($rootFiles.Count -gt 0) {
            # In 1.21.3+, to include files in the root of textures/ without adding subdirectories,
            # we must use type: single for each file, or just type: directory with "" source,
            # but "" source includes subdirectories. Let's just use single for root files.
            foreach ($file in $rootFiles) {
                $name = $file.BaseName
                $sources += @{
                    type = "single"
                    resource = "$($ns.Name):$name"
                    sprite = "$($ns.Name):$name"
                }
            }
        }
        
        # Add all directories
        foreach ($d in $dirs) {
            $relPath = $d.FullName.Substring($texDir.Length + 1).Replace('\', '/')
            # Skip standard directories that are auto-included by vanilla IF it's the minecraft namespace
            if ($ns.Name -eq "minecraft" -and ($relPath -eq "item" -or $relPath -eq "block")) {
                continue
            }
            
            $sources += @{
                type = "directory"
                source = $relPath
                prefix = "$relPath/"
            }
        }
        
        $json = @{
            sources = $sources
        } | ConvertTo-Json -Depth 10
        
        Set-Content -Path (Join-Path $atlasDir "blocks.json") -Value $json
    }
}
