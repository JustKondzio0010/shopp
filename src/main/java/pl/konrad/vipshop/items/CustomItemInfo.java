package pl.konrad.vipshop.items;

import org.bukkit.Material;
import java.util.List;

public final class CustomItemInfo {
    public final String id;
    public final Material material;
    public final String name;
    public final int customModelData;
    public final List<String> lore;
    public final double price;

    public CustomItemInfo(String id, Material material, String name, int customModelData, List<String> lore, double price) {
        this.id = id;
        this.material = material;
        this.name = name;
        this.customModelData = customModelData;
        this.lore = lore;
        this.price = price;
    }
}
