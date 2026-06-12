package pl.konrad.vipshop.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import pl.konrad.vipshop.VipShop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

public final class CustomItemManager {

    private final VipShop plugin;
    private final Map<String, CustomItemInfo> registry = new LinkedHashMap<>();
    
    public static NamespacedKey ITEM_ID_KEY;
    public static NamespacedKey BACKPACK_UUID_KEY;

    public CustomItemManager(VipShop plugin) {
        this.plugin = plugin;
        ITEM_ID_KEY = new NamespacedKey(plugin, "custom_item_id");
        BACKPACK_UUID_KEY = new NamespacedKey(plugin, "backpack_uuid");
        registerItems();
    }

    private void registerItems() {
        // Mining Tools
        add("miner_pickaxe", Material.DIAMOND_PICKAXE, "§e§lKilof Górnika (1x2)", 1001,
            List.of("§7Specjalny kilof niszczący", "§72 bloki pionowo naraz.", "§7Kopie automatycznie przy normalnym łamaniu bloków.", "", "§a● Kopanie pionowe 1x2"));
            
        add("compact_hammer", Material.DIAMOND_PICKAXE, "§e§lMłot Kompaktowy (2x2)", 1002,
            List.of("§7Lekki młot niszczący", "§7obszar 2x2 wokół kopanego bloku.", "§7Kopie automatycznie przy normalnym łamaniu bloków.", "", "§a● Kopanie obszarowe 2x2"));

        add("tunnel_destroyer", Material.NETHERITE_PICKAXE, "§6§lNiszczyciel Tuneli (3x3)", 1003,
            List.of("§7Potężny młot niszczący", "§7obszar 3x3 wokół kopanego bloku.", "§7Kopie automatycznie przy normalnym łamaniu bloków.", "", "§6● Kopanie obszarowe 3x3", "§c● Kara: -15% do szybkości bicia"));

        add("titan_excavator", Material.NETHERITE_PICKAXE, "§c§lEkskawator Tytanowy (4x4)", 1004,
            List.of("§cEnd-game'owa zabawka!", "§7Niszczy potężny obszar 4x4.", "§7Kopie automatycznie przy normalnym łamaniu bloków.", "§7Kopie w bezpieczny, zoptymalizowany sposób.", "", "§c● Kopanie obszarowe 4x4", "§c● Kara: -15% do szybkości bicia"));

        add("titan_pickaxe", Material.NETHERITE_PICKAXE, "§e§lTytanowy Kilof", 1005,
            List.of("§7Zwykły kilof 1x1, lecz posiada", "§7wyjątkową, wieczną wytrzymałość.", "", "§d● Efficiency V", "§d● Niezniszczalność (Unbreakable)"));

        add("gold_vein_pickaxe", Material.DIAMOND_PICKAXE, "§e§lKilof Złota Żyła", 1006,
            List.of("§7Kilof z głębin kopalni.", "§7Podwaja drop ze wszystkich rud.", "", "§d● Fortuna IV"));

        add("greed_artifact", Material.DIAMOND_PICKAXE, "§c§lArtefakt Chciwości", 1007,
            List.of("§cKlątwa Chciwości!", "§7Oferuje niespotykaną fortunę,", "§7lecz posiada klątwę wiązania.", "", "§d● Fortuna V", "§c● Curse of Binding"));

        add("smelt_drill", Material.DIAMOND_PICKAXE, "§e§lWiertło Hutnika", 1008,
            List.of("§7Posiada wbudowany palnik.", "§7Automatycznie przetapia wykopane rudy", "§7żelaza, złota i miedzi na sztabki.", "", "§d● Autoprzetapianie", "§d● Fortuna III"));

        // Weapons
        add("collector_sword", Material.DIAMOND_SWORD, "§a§lMiecz Zbieracza", 1009,
            List.of("§7Zaprojektowany do farm mobów.", "§7Wyciąga ogromną ilość łupów.", "", "§d● Grabież IV"));

        add("butcher_sword", Material.NETHERITE_SWORD, "§c§lOstrze Rzeźnika", 1010,
            List.of("§cBroń ostateczna!", "§7Zabija niemal wszystko jednym ciosem", "§7i gwarantuje gigantyczny drop.", "", "§d● Sharpness V", "§d● Grabież V"));

        add("titan_sword", Material.NETHERITE_SWORD, "§c§lTytanowy Miecz", 1025,
            List.of("§7Ostrze z najczystszego tytanu.", "§7Potężne obrażenia i trwałość.", "", "§d● Sharpness VI", "§d● Fire Aspect III"));

        add("reaper_scythe", Material.NETHERITE_SWORD, "§6§lKosa Żniwiarza", 1011,
            List.of("§7Tnie hordy wrogów jednocześnie.", "§7Zadaje obrażenia automatycznie przy każdym uderzeniu.", "§7Zadaje pełne obrażenia wszystkim mobom", "§7w promieniu 3 bloków od celu.", "", "§6● Obrażenia obszarowe (AOE)"));

        add("vampire_dagger", Material.IRON_SWORD, "§d§lSztylet Wampira", 1012,
            List.of("§7Zadaje mniejsze obrażenia,", "§7lecz wysysa życie z przeciwników.", "", "§d● Kradzież zdrowia (Life Steal): +1 serce za hit"));

        add("headhunter_sword", Material.DIAMOND_SWORD, "§e§lMiecz Łowcy Głów", 1013,
            List.of("§7Zabójcza precyzja.", "§7Zabijanie mobów daje szansę na drop", "§7ich kolekcjonerskich głów.", "", "§d● 5% szans na głowę moba"));

        // Armor
        add("titan_helmet", Material.NETHERITE_HELMET, "§e§lTytanowy Hełm", 1014,
            List.of("§7Hełm z tytanowego setu.", "", "§d● Ochrona V", "§d● Niezniszczalność"));
            
        add("titan_chestplate", Material.NETHERITE_CHESTPLATE, "§e§lTytanowy Napięstnik", 1015,
            List.of("§7Napięstnik z tytanowego setu.", "", "§d● Ochrona V", "§d● Niezniszczalność"));

        add("titan_leggings", Material.NETHERITE_LEGGINGS, "§e§lTytanowe Nogawice", 1016,
            List.of("§7Nogawice z tytanowego setu.", "", "§d● Ochrona V", "§d● Niezniszczalność"));

        add("titan_boots", Material.NETHERITE_BOOTS, "§e§lTytanowe Buty", 1017,
            List.of("§7Buty z tytanowego setu.", "", "§d● Ochrona V", "§d● Niezniszczalność"));

        add("hermes_boots", Material.GOLDEN_BOOTS, "§e§lButy Hermesa", 1018,
            List.of("§7Dają niespotykaną lekkość i szybkość.", "", "§e● Pasywna Szybkość II", "§e● Brak obrażeń od upadku (Feather Falling V)"));

        add("nightvision_helmet", Material.DIAMOND_HELMET, "§e§lHełm Ciemności", 1019,
            List.of("§7Hełm rozświetlający najgłębsze mroki.", "", "§e● Pasywne Widzenie w Ciemności"));

        add("magma_chestplate", Material.NETHERITE_CHESTPLATE, "§6§lPancerz Magmy", 1020,
            List.of("§7Gorąca jak lawa klatka piersiowa.", "", "§e● 100% odporności na ogień i lawę"));

        // Utility & Farming
        add("lumberjack_axe", Material.DIAMOND_AXE, "§a§lTopór Drwala", 1021,
            List.of("§7Ścina automatycznie przy złamaniu pnia drzewa.", "§7Niszczy pień i liście jednym uderzeniem.", "", "§a● Szybkie ścinanie (TreeFeller)"));

        add("harvest_hoe", Material.DIAMOND_HOE, "§a§lMotyka Obfitości", 1022,
            List.of("§7Narzędzie każdego rolnika.", "§7Kliknij §aPRAWYM przyciskiem §7na farmlandzie, aby zebrać plony 3x3.", "§7i automatycznie zasadzić nowe z ekwipunku.", "", "§a● Zbiór i siew 3x3"));

        add("magnetic_rod", Material.FISHING_ROD, "§b§lMagnetyczna Wędka", 1023,
            List.of("§7Niezwykle szybki połów.", "", "§d● Lure III", "§d● Luck of the Sea IV", "§b● Znacznie szybsze branie ryb"));

        add("bottomless_backpack", Material.CHEST, "§d§lPlecak Bez Dna", 1024,
            List.of("§7Podręczny schowek o pojemności", "§7podwójnej skrzyni (54 sloty).", "", "§d● Przenośne inventory", "§7Kliknij prawym przyciskiem, aby otworzyć."));

        // Amulets
        add("amulet_berserker", Material.BLAZE_POWDER, "Amulet Berserkera", 2001,
            List.of("§7Pasywny: HP < 40%", "§c● Stały Strength II + Speed I", "", "§7Klimatyczny artefakt nasycony furią,", "§7budzący dzikość wojownika w chwili zagrożenia."));

        add("amulet_phantom", Material.ENDER_PEARL, "Amulet Widma", 2002,
            List.of("§7Aktywny: PPM — Dash do przodu (6 bloków)", "§5● Cooldown: 10s", "", "§7Pozwala na niematerialne przemieszczanie się,", "§7zostawiając za sobą astralny ogon."));

        add("amulet_vampire", Material.FERMENTED_SPIDER_EYE, "Amulet Wampira", 2003,
            List.of("§7Pasywny: co 20s wysysa 2HP z moba", "§4● Tylko wrogie moby i Gracze", "§7Promień: 6 bloków", "", "§7Nasycony mroczną potęgą krwawych lordów."));

        add("amulet_seismic", Material.WHITE_DYE, "Amulet Sejsmiczny", 2004,
            List.of("§7Pasywny: Haste II co 3s", "§e● Działa tylko pod ziemią (kamień pod stopami)", "§7Odświeżenie co 3s", "", "§7Rezonuje z wibracjami podziemi."));

        add("amulet_gold_vein", Material.GOLD_NUGGET, "Amulet Złotej Żyły", 2005,
            List.of("§7Aktywny: PPM — Skanuje rudy", "§7w promieniu 100 bloków", "§6● Skanuje Asynchronicznie", "§7Cooldown: 30s", "", "§7Prowadzi chciwych wprost do skarbów."));

        add("amulet_wind", Material.FEATHER, "Amulet Wiatru", 2006,
            List.of("§7Aktywny: PPM — Speed III + Jump Boost III na 8s", "§b● Wyłączony w wodzie", "§7Nie stackuje się z Butami Hermesa", "§7Cooldown: 10s", "", "§7Uosobienie samej lekkości chmur."));

        add("amulet_shadow", Material.INK_SAC, "Amulet Cienia", 2007,
            List.of("§7Aktywny: PPM — Invisibility + Speed II na 3s", "§8● Zbroja nadal widoczna", "§7Cooldown: 25s", "", "§7Spowija ciało płaszczem gęstego mroku."));

        add("amulet_greed", Material.EMERALD, "Amulet Chciwca", 2008,
            List.of("§7Pasywny: Luck I co 10s na 4s", "§a● Zwiększa drop rate z mobów", "", "§7Uśmiech losu dla tych, którzy ryzykują najwięcej."));

        add("amulet_necromancer", Material.WITHER_SKELETON_SKULL, "Amulet Nekromanty", 2009,
            List.of("§7Aktywny: PPM — Przywołuje Zombie na 8s", "§5● Zombie atakuje Wrogie Moby i Graczy", "§5● Otrzymuje stały Speed I", "§7Cooldown: 60s", "", "§7Zaklęty symbol władzy nad nieumarłymi."));

        add("amulet_time", Material.CLOCK, "Amulet Czasu", 2010,
            List.of("§7Aktywny: PPM — Zatrzymuje Czas", "§3● Gracze: Freeze 1.5s + Slowness III na 2s", "§3● Moby: Freeze 3s + Slowness III na 4s", "§7Promień: 5 bloków", "§7Cooldown: 35s", "", "§7Zakrzywia linię czasoprzestrzenną oponentów."));

        add("amulet_volcano", Material.MAGMA_CREAM, "Amulet Wulkanu", 2011,
            List.of("§7Aktywny: PPM — Fala ognia (8 bloków)", "§c● Zadaje 3 damage i podpala na 4s", "§7Cooldown: 35s", "", "§7Kawałek skondensowanej furii z wnętrza ziemi."));

        add("amulet_tide", Material.HEART_OF_THE_SEA, "Amulet Przypływu", 2012,
            List.of("§7Pasywny: Działa tylko w wodzie", "§b● Dolphins Grace, Night Vision, Wodne Oddychanie", "§7Permanentnie dopóki jesteś zanurzony", "", "§7Tętni chłodem oceanicznych głębin."));

        add("amulet_illusion", Material.ARMOR_STAND, "Amulet Iluzji", 2013,
            List.of("§7Aktywny: PPM — Tworzy 3 fałszywe kopie na 5s", "§d● Moby atakują kopie zamiast Ciebie", "§7Promień: 5 bloków", "§7Cooldown: 35s", "", "§7Zaburza percepcję wrogów, pozostawiając zgliszcza umysłu."));

        add("amulet_tsunami", Material.PRISMARINE_SHARD, "Amulet Tsunami", 2014,
            List.of("§7Aktywny: PPM — Wodna fala odrzucająca", "§9● Popycha wrogów o 8 bloków", "§7Czas trwania fali: 2s", "§7Cooldown: 60s", "", "§7Niszczycielska siła oceanu ukryta w krysztale."));
    }

    private void add(String id, Material material, String name, int customModelData, List<String> lore) {
        double defaultPrice = 10000.0;
        if (id.equals("titan_helmet") || id.equals("titan_chestplate") || id.equals("titan_leggings") || id.equals("titan_boots")) {
            defaultPrice = 250000.0;
        }
        double price = plugin.getConfig().getDouble("shop." + id, defaultPrice);
        registry.put(id, new CustomItemInfo(id, material, name, customModelData, lore, price));
    }

    public Collection<CustomItemInfo> getRegisteredItems() {
        return registry.values();
    }

    public CustomItemInfo getItemInfo(String id) {
        return registry.get(id);
    }

    public ItemStack createItem(String id) {
        CustomItemInfo info = registry.get(id);
        if (info == null) {
            return null;
        }

        ItemStack item = new ItemStack(info.material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(getGradientName(id)).decoration(TextDecoration.ITALIC, false));
        
        List<Component> compLore = new ArrayList<>();
        for (String line : info.lore) {
            compLore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
        }
        // Usunięto linię z ceną z przedmiotu
        meta.lore(compLore);
        meta.setCustomModelData(info.customModelData);

        // Enchanty wg. wymagań:
        switch(id) {
            case "miner_pickaxe", "compact_hammer" -> {
                meta.addEnchant(Enchantment.DIG_SPEED, 4, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
            }
            case "tunnel_destroyer", "titan_excavator" -> {
                meta.addEnchant(Enchantment.DIG_SPEED, 5, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
            }
            case "smelt_drill" -> {
                meta.addEnchant(Enchantment.DIG_SPEED, 4, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
                meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 2, true);
            }
            case "gold_vein_pickaxe" -> {
                meta.addEnchant(Enchantment.DIG_SPEED, 5, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
                meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 4, true);
            }
            case "greed_artifact" -> {
                meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 5, true);
                meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            }
            case "collector_sword" -> {
                meta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
                meta.addEnchant(Enchantment.LOOT_BONUS_MOBS, 4, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
            }
            case "butcher_sword" -> {
                meta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
                meta.addEnchant(Enchantment.LOOT_BONUS_MOBS, 5, true);
                meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
            }
            case "titan_sword" -> {
                meta.addEnchant(Enchantment.DAMAGE_ALL, 6, true);
                meta.addEnchant(Enchantment.FIRE_ASPECT, 3, true);
                meta.addEnchant(Enchantment.DURABILITY, 5, true);
            }
            case "reaper_scythe" -> {
                meta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
                meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
                meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
            }
            case "vampire_dagger" -> {
                meta.addEnchant(Enchantment.DAMAGE_ALL, 3, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
            }
            case "headhunter_sword" -> {
                meta.addEnchant(Enchantment.DAMAGE_ALL, 4, true);
                meta.addEnchant(Enchantment.LOOT_BONUS_MOBS, 3, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
            }
            case "lumberjack_axe" -> {
                meta.addEnchant(Enchantment.DIG_SPEED, 5, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
                meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 2, true);
            }
            case "harvest_hoe" -> {
                meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 2, true);
                meta.addEnchant(Enchantment.DURABILITY, 4, true);
                meta.addEnchant(Enchantment.DIG_SPEED, 4, true);
            }
            case "magnetic_rod" -> {
                meta.addEnchant(Enchantment.LURE, 3, true);
                meta.addEnchant(Enchantment.LUCK, 4, true);
                meta.addEnchant(Enchantment.DURABILITY, 4, true);
            }
            case "titan_helmet", "titan_chestplate", "titan_leggings", "titan_boots" -> {
                meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 5, true);
                meta.addEnchant(Enchantment.DURABILITY, 4, true);
                meta.addEnchant(Enchantment.MENDING, 1, true);
            }
            case "hermes_boots" -> {
                meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
                meta.addEnchant(Enchantment.PROTECTION_FALL, 5, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
                meta.addEnchant(Enchantment.MENDING, 1, true);
            }
            case "nightvision_helmet" -> {
                meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
                meta.addEnchant(Enchantment.MENDING, 1, true);
            }
            case "magma_chestplate" -> {
                meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
                meta.addEnchant(Enchantment.MENDING, 1, true);
                meta.addEnchant(Enchantment.PROTECTION_FIRE, 5, true);
            }
        }
        
        if (id.startsWith("amulet_")) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
        }

        // Apply Custom NBT Identifier
        meta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.STRING, id);
        
        // Generate backpack UUID if it is a backpack
        if (id.equals("bottomless_backpack")) {
            String backpackUuid = UUID.randomUUID().toString();
            meta.getPersistentDataContainer().set(BACKPACK_UUID_KEY, PersistentDataType.STRING, backpackUuid);
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.STRING);
    }

    public String getBackpackUuid(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(BACKPACK_UUID_KEY, PersistentDataType.STRING);
    }

    public String getGradientName(String id) {
        return switch (id) {
            // Equipment
            case "miner_pickaxe" -> "<gradient:#aaaaaa:#ffffff><bold>Kilof Górnika</bold></gradient>";
            case "compact_hammer" -> "<gradient:#aaaaaa:#ffffff><bold>Młot Kompaktowy</bold></gradient>";
            case "tunnel_destroyer" -> "<gradient:#ffaa00:#ffff55><bold>Niszczyciel Tuneli</bold></gradient>";
            case "titan_excavator" -> "<gradient:#ff4400:#ffaa00><bold>Ekskawator Tytanowy</bold></gradient>";
            case "titan_pickaxe" -> "<gradient:#ffaa00:#ffff55><bold>Tytanowy Kilof</bold></gradient>";
            case "gold_vein_pickaxe" -> "<gradient:#ffaa00:#ffff55><bold>Kilof Złota Żyła</bold></gradient>";
            case "greed_artifact" -> "<gradient:#aa00ff:#ff00aa><bold>Artefakt Chciwości</bold></gradient>";
            case "smelt_drill" -> "<gradient:#ff4400:#ffaa00><bold>Wiertło Hutnika</bold></gradient>";
            case "collector_sword" -> "<gradient:#00aaff:#00ffff><bold>Miecz Zbieracza</bold></gradient>";
            case "butcher_sword" -> "<gradient:#aa0000:#ff5555><bold>Ostrze Rzeźnika</bold></gradient>";
            case "titan_sword" -> "<gradient:#ffaa00:#ffff55><bold>Tytanowy Miecz</bold></gradient>";
            case "reaper_scythe" -> "<gradient:#550000:#aa0000><bold>Kosa Żniwiarza</bold></gradient>";
            case "vampire_dagger" -> "<gradient:#aa0000:#ff0000><bold>Sztylet Wampira</bold></gradient>";
            case "headhunter_sword" -> "<gradient:#aaaaaa:#ffffff><bold>Miecz Łowcy Głów</bold></gradient>";
            case "titan_helmet" -> "<gradient:#aaaaaa:#ffffff><bold>Tytanowy Hełm</bold></gradient>";
            case "titan_chestplate" -> "<gradient:#aaaaaa:#ffffff><bold>Tytanowy Napięstnik</bold></gradient>";
            case "titan_leggings" -> "<gradient:#aaaaaa:#ffffff><bold>Tytanowe Nogawice</bold></gradient>";
            case "titan_boots" -> "<gradient:#aaaaaa:#ffffff><bold>Tytanowe Buty</bold></gradient>";
            case "hermes_boots" -> "<gradient:#ffff55:#ffffff><bold>Buty Hermesa</bold></gradient>";
            case "nightvision_helmet" -> "<gradient:#aa00ff:#5500aa><bold>Hełm Ciemności</bold></gradient>";
            case "magma_chestplate" -> "<gradient:#ff4400:#ffaa00><bold>Pancerz Magmy</bold></gradient>";
            case "lumberjack_axe" -> "<gradient:#00aa00:#00ff00><bold>Topór Drwala</bold></gradient>";
            case "harvest_hoe" -> "<gradient:#00ff00:#ffff55><bold>Motyka Obfitości</bold></gradient>";
            case "magnetic_rod" -> "<gradient:#00aaff:#00ffff><bold>Magnetyczna Wędka</bold></gradient>";
            case "bottomless_backpack" -> "<gradient:#aa00ff:#ff00aa><bold>Plecak Bez Dna</bold></gradient>";
            
            // Amulets
            case "amulet_berserker" -> "<gradient:#ff0000:#ff5555><bold>Amulet Berserkera</bold></gradient>";
            case "amulet_phantom" -> "<gradient:#aa00ff:#5500aa><bold>Amulet Widma</bold></gradient>";
            case "amulet_vampire" -> "<gradient:#aa0000:#550000><bold>Amulet Wampira</bold></gradient>";
            case "amulet_seismic" -> "<gradient:#ffff55:#ffaa00><bold>Amulet Sejsmiczny</bold></gradient>";
            case "amulet_gold_vein" -> "<gradient:#ffaa00:#ffff55><bold>Amulet Złotej Żyły</bold></gradient>";
            case "amulet_wind" -> "<gradient:#00ffff:#ffffff><bold>Amulet Wiatru</bold></gradient>";
            case "amulet_shadow" -> "<gradient:#888888:#555555><bold>Amulet Cienia</bold></gradient>";
            case "amulet_greed" -> "<gradient:#55ff55:#00aa00><bold>Amulet Chciwca</bold></gradient>";
            case "amulet_necromancer" -> "<gradient:#aa00ff:#ff00ff><bold>Amulet Nekromanty</bold></gradient>";
            case "amulet_time" -> "<gradient:#00aaff:#0055aa><bold>Amulet Czasu</bold></gradient>";
            case "amulet_volcano" -> "<gradient:#ff0000:#ffaa00><bold>Amulet Wulkanu</bold></gradient>";
            case "amulet_tide" -> "<gradient:#00aaff:#00ffff><bold>Amulet Przypływu</bold></gradient>";
            case "amulet_illusion" -> "<gradient:#ffaa00:#ff00aa><bold>Amulet Iluzji</bold></gradient>";
            case "amulet_tsunami" -> "<gradient:#0000aa:#00aaff><bold>Amulet Tsunami</bold></gradient>";
            
            default -> "<gradient:#aaaaaa:#ffffff><bold>Przedmiot VIP</bold></gradient>";
        };
    }

    // ==========================================
    // Inventory Base64 Serialization Methods
    // ==========================================

    public String toBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Blad serializacji ekwipunku plecaka.", e);
        }
    }

    public ItemStack[] fromBase64(String data) throws java.io.IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new java.io.IOException("Blad deseryalizacji: nie znaleziono klasy ItemStack.", e);
        }
    }
}
