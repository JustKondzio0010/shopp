 = "C:\Users\konra\OneDrive\Pulpit\vipshop\Custom\assets\minecraft\models\item"
 = "C:\Users\konra\OneDrive\Pulpit\vipshop\Custom\assets\minecraft\items"

if (-not (Test-Path )) {
    New-Item -ItemType Directory -Path  | Out-Null
}

 = Get-ChildItem -Path  -Filter "*.json"
foreach ( in ) {
     = Get-Content .FullName -Raw | ConvertFrom-Json
    if ( -ne .overrides) {
         = .BaseName
         = @()
        foreach ( in .overrides) {
            if ( -ne .predicate.custom_model_data) {
                 = .predicate.custom_model_data
                 = .model
                if (-not .Contains(":")) {
                     = "minecraft:" + 
                }
                
                 += @{
                    when = [string]
                    model = @{
                        type = "minecraft:model"
                        model = 
                    }
                }
            }
        }
        
         = "minecraft:item/"
        
         = @{
            model = @{
                type = "minecraft:select"
                property = "minecraft:custom_model_data"
                fallback = @{
                    type = "minecraft:model"
                    model = 
                }
                cases = 
            }
        }
        
         = Join-Path  (.Name)
         | ConvertTo-Json -Depth 10 | Set-Content 
        Write-Host "Created "
    }
}
