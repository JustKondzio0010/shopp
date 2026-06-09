$base = "C:\Users\konra\OneDrive\Pulpit\vipshop\Custom\assets"
$namespaces = Get-ChildItem -Path $base -Directory

foreach ($ns in $namespaces) {
    if ($ns.Name -eq "minecraft") { continue }
    
    $texDir = Join-Path $ns.FullName "textures"
    if (Test-Path $texDir) {
        $atlasDir = Join-Path $ns.FullName "atlases"
        if (-not (Test-Path $atlasDir)) {
            New-Item -ItemType Directory -Force -Path $atlasDir | Out-Null
        }
        
        $files = Get-ChildItem -Path $texDir -File -Filter "*.png" -Recurse
        $sources = @()
        
        foreach ($f in $files) {
            $relPath = $f.FullName.Substring($texDir.Length + 1).Replace('\', '/')
            $resourcePath = $relPath.Substring(0, $relPath.Length - 4) # remove .png
            
            $sources += @{
                type = "single"
                resource = "$($ns.Name):$resourcePath"
                sprite = "$($ns.Name):$resourcePath"
            }
        }
        
        $json = @{
            sources = $sources
        } | ConvertTo-Json -Depth 10
        
        Set-Content -Path (Join-Path $atlasDir "blocks.json") -Value $json
    }
}
