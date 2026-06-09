import os
import json

models_dir = r"C:\Users\konra\OneDrive\Pulpit\vipshop\Custom\assets\minecraft\models\item"
items_dir = r"C:\Users\konra\AppData\Roaming\.minecraft\resourcepacks\Custom\assets\minecraft\items"

if not os.path.exists(items_dir):
    os.makedirs(items_dir)

for filename in os.listdir(models_dir):
    if not filename.endswith(".json"): continue
    
    filepath = os.path.join(models_dir, filename)
    with open(filepath, "r", encoding="utf-8") as f:
        try:
            data = json.load(f)
        except:
            continue
            
    if "overrides" in data:
        basename = filename[:-5]
        entries = []
        for override in data["overrides"]:
            if "predicate" in override and "custom_model_data" in override["predicate"]:
                cmd = float(override["predicate"]["custom_model_data"])
                model = override["model"]
                if ":" not in model:
                    model = "minecraft:" + model
                    
                entries.append({
                    "threshold": cmd,
                    "model": {
                        "type": "minecraft:model",
                        "model": model
                    }
                })
        
        if entries:
            # Sort entries by threshold as required by range_dispatch
            entries.sort(key=lambda x: x["threshold"])
            
            fallback = f"minecraft:item/{basename}"
            new_item = {
                "model": {
                    "type": "minecraft:range_dispatch",
                    "property": "minecraft:custom_model_data",
                    "fallback": {
                        "type": "minecraft:model",
                        "model": fallback
                    },
                    "entries": entries
                }
            }
            
            outpath = os.path.join(items_dir, filename)
            with open(outpath, "w", encoding="utf-8") as outf:
                json.dump(new_item, outf, indent=2)
            print(f"Created {outpath}")
