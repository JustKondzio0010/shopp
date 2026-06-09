import os
import json

base_dir = r"C:\Users\konra\AppData\Roaming\.minecraft\resourcepacks\Custom\assets"
namespaces = [d for d in os.listdir(base_dir) if os.path.isdir(os.path.join(base_dir, d))]

for namespace in namespaces:
    textures_dir = os.path.join(base_dir, namespace, "textures")
    if not os.path.isdir(textures_dir):
        continue
        
    atlases_dir = os.path.join(base_dir, namespace, "atlases")
    if not os.path.exists(atlases_dir):
        os.makedirs(atlases_dir)
        
    sources = []
    
    # Add root of textures just in case
    sources.append({
        "type": "directory",
        "source": "",
        "prefix": ""
    })
    
    # Add every subdirectory
    for d in os.listdir(textures_dir):
        if os.path.isdir(os.path.join(textures_dir, d)):
            sources.append({
                "type": "directory",
                "source": d,
                "prefix": d + "/"
            })
            
    blocks_json = {"sources": sources}
    
    outpath = os.path.join(atlases_dir, "blocks.json")
    with open(outpath, "w", encoding="utf-8") as outf:
        json.dump(blocks_json, outf, indent=2)
    print(f"Created {outpath}")
