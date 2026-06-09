package pl.konrad.vipshop.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import pl.konrad.vipshop.VipShop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AmuletListener implements Listener {

    private final VipShop plugin;
    private final Random random = new Random();

    // Mapy cooldownów (Głównie dla Aktywnych, ale i Pasywne używają by nie zasypywać ticków)
    private final Map<UUID, Long> cdBerserker = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cdPhantom = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cdVampire = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cdGoldVein = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cdWind = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cdShadow = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cdGreed = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cdNecromancer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cdTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cdVolcano = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cdIllusion = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cdTsunami = new ConcurrentHashMap<>();
    
    private final Map<UUID, Long> activeIllusions = new ConcurrentHashMap<>();

    public AmuletListener(VipShop plugin) {
        this.plugin = plugin;
        startPassiveTasks();
        startActionBarTasks();
    }

    private void startPassiveTasks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) {
                        continue;
                    }

                    ItemStack main = player.getInventory().getItemInMainHand();
                    ItemStack off = player.getInventory().getItemInOffHand();
                    
                    String mainId = plugin.getCustomItemManager().getItemId(main);
                    String offId = plugin.getCustomItemManager().getItemId(off);
                    
                    boolean hasBerserker = "amulet_berserker".equals(mainId) || "amulet_berserker".equals(offId);
                    boolean hasVampire = "amulet_vampire".equals(mainId) || "amulet_vampire".equals(offId);
                    boolean hasSeismic = "amulet_seismic".equals(mainId) || "amulet_seismic".equals(offId);
                    boolean hasWind = false; // Moved to active
                    boolean hasGreed = "amulet_greed".equals(mainId) || "amulet_greed".equals(offId);
                    boolean hasTide = "amulet_tide".equals(mainId) || "amulet_tide".equals(offId);

                    // 11. Amulet Przypływu
                    if (hasTide) {
                        if (player.isInWater()) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 60, 0, false, false, false));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 260, 0, false, false, false));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 60, 0, false, false, false));
                        }
                    }

                    // 1. Amulet Berserkera (HP < 40%, ciągły buff)
                    if (hasBerserker) {
                        double maxHp = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                        if (player.getHealth() < maxHp * 0.4) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 1, false, false, false));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false, false));
                            
                            if (player.getTicksLived() % 20 == 0) {
                                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.05);
                                player.getWorld().spawnParticle(Particle.LAVA, player.getLocation().add(0, 2, 0), 5, 0.3, 0.1, 0.3, 0.05);
                            }
                        }
                    }

                    // 3. Amulet Wampira (Wysysa 2HP co 20s z wrogich mobów i graczy)
                    if (hasVampire) {
                        if (now >= cdVampire.getOrDefault(player.getUniqueId(), 0L)) {
                            List<LivingEntity> targets = new ArrayList<>();
                            for (Entity e : player.getNearbyEntities(6.0, 6.0, 6.0)) {
                                if (e instanceof LivingEntity le && e != player && e.getType() != EntityType.ARMOR_STAND) {
                                    if (le instanceof org.bukkit.entity.Monster || le instanceof Player) {
                                        targets.add(le);
                                    }
                                }
                            }
                            if (!targets.isEmpty()) {
                                LivingEntity target = targets.get(random.nextInt(targets.size()));
                                target.damage(2.0, player);
                                
                                double maxHp = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                                player.setHealth(Math.min(maxHp, player.getHealth() + 2.0));
                                
                                target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
                                target.getWorld().spawnParticle(Particle.DRIP_LAVA, target.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.1);
                                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 5, 0.3, 0.1, 0.3, 0.1);
                                player.playSound(player.getLocation(), Sound.ENTITY_BAT_HURT, 0.7f, 0.5f);
                                
                                cdVampire.put(player.getUniqueId(), now + 20000L);
                            }
                        }
                    }

                    // 4. Amulet Sejsmiczny (Pasywny Haste dopóki trzymamy)
                    if (hasSeismic) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 40, 1, false, false, false));
                    }

                    // 6. Amulet Wiatru przeniesiony do aktywnych!

                    // 8. Amulet Chciwca (Luck I co 10s na 4s)
                    if (hasGreed) {
                        if (now >= cdGreed.getOrDefault(player.getUniqueId(), 0L)) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 80, 0, false, false, false));
                            
                            player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.05);
                            player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation().add(0, 2, 0), 5, 0.3, 0.1, 0.3, 0.05);
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 0.5f, 1.2f);
                            
                            cdGreed.put(player.getUniqueId(), now + 10000L);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Co sekundę
    }
    
    // ActionBar update dla Aktywnych Amuletów informujący o Cooldownie
    private void startActionBarTasks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ItemStack main = player.getInventory().getItemInMainHand();
                    String id = plugin.getCustomItemManager().getItemId(main);
                    if (id == null) continue;
                    
                    long cdEnd = 0;
                    String name = "";
                    
                    switch (id) {
                        case "amulet_phantom" -> { cdEnd = cdPhantom.getOrDefault(player.getUniqueId(), 0L); name = "Widma"; }
                        case "amulet_gold_vein" -> { cdEnd = cdGoldVein.getOrDefault(player.getUniqueId(), 0L); name = "Złotej Żyły"; }
                        case "amulet_wind" -> { cdEnd = cdWind.getOrDefault(player.getUniqueId(), 0L); name = "Wiatru"; }
                        case "amulet_shadow" -> { cdEnd = cdShadow.getOrDefault(player.getUniqueId(), 0L); name = "Cienia"; }
                        case "amulet_necromancer" -> { cdEnd = cdNecromancer.getOrDefault(player.getUniqueId(), 0L); name = "Nekromanty"; }
                        case "amulet_time" -> { cdEnd = cdTime.getOrDefault(player.getUniqueId(), 0L); name = "Czasu"; }
                        case "amulet_volcano" -> { cdEnd = cdVolcano.getOrDefault(player.getUniqueId(), 0L); name = "Wulkanu"; }
                        case "amulet_illusion" -> { cdEnd = cdIllusion.getOrDefault(player.getUniqueId(), 0L); name = "Iluzji"; }
                        case "amulet_tsunami" -> { cdEnd = cdTsunami.getOrDefault(player.getUniqueId(), 0L); name = "Tsunami"; }
                        default -> { continue; }
                    }
                    
                    if (cdEnd > now) {
                        long left = (cdEnd - now) / 1000;
                        player.sendActionBar(Component.text("Odnowienie (Amulet " + name + "): " + left + "s", NamedTextColor.RED));
                    } else {
                        player.sendActionBar(Component.text("Amulet " + name + " gotowy!", NamedTextColor.GREEN));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    // AKTYWNE AMULETY (PPM)
    @EventHandler(priority = EventPriority.HIGH)
    public void onActiveAmuletInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack item = event.getItem();
        String id = plugin.getCustomItemManager().getItemId(item);
        if (id == null) return;

        long now = System.currentTimeMillis();

        // 2. Amulet Widma (Dash)
        if (id.equals("amulet_phantom")) {
            event.setCancelled(true);
            if (now < cdPhantom.getOrDefault(player.getUniqueId(), 0L)) {
                return;
            }
            
            Location startLoc = player.getLocation();
            Vector dir = startLoc.getDirection().normalize();
            
            // Kolizja - sprawdzamy bloki na trasie 6 blokow (kroki co 0.5)
            Location targetLoc = startLoc.clone();
            for (double i = 0; i <= 6.0; i += 0.5) {
                Location check = startLoc.clone().add(dir.clone().multiply(i));
                if (!check.getBlock().isPassable()) {
                    break;
                }
                targetLoc = check;
            }
            
            // Dash velocity - 2.5 mnoznik mocy
            Vector velocity = dir.clone().multiply(2.5);
            velocity.setY(0.1); // Zapobiega mocnemu uderzeniu o podloge
            player.setVelocity(velocity);
            
            player.getWorld().spawnParticle(Particle.END_ROD, startLoc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            player.getWorld().spawnParticle(Particle.PORTAL, targetLoc.add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.5);
            player.playSound(startLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
            
            new BukkitRunnable() {
                int ticks = 0;
                Location trail = player.getLocation();
                @Override
                public void run() {
                    if (ticks >= 10 || !player.isOnline()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                        cancel();
                        return;
                    }
                    if (ticks % 2 == 0) {
                        player.getWorld().spawnParticle(Particle.PORTAL, trail.clone().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
                    }
                    trail = player.getLocation();
                    ticks++;
                }
            }.runTaskTimer(plugin, 1L, 1L);
            
            cdPhantom.put(player.getUniqueId(), now + 10000L);
        }
        
        // 5. Amulet Złotej Żyły (Skaner 100 bloków async)
        else if (id.equals("amulet_gold_vein")) {
            event.setCancelled(true);
            if (now < cdGoldVein.getOrDefault(player.getUniqueId(), 0L)) {
                return;
            }
            
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.5f);
            player.sendMessage(Component.text("Rozpoczęto skanowanie w promieniu 40 bloków...", NamedTextColor.YELLOW));
            
            Location loc = player.getLocation();
            org.bukkit.World world = loc.getWorld();
            int cx = loc.getBlockX();
            int cy = loc.getBlockY();
            int cz = loc.getBlockZ();
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    List<Location> ores = new ArrayList<>();
                    // Ograniczamy Y do strefy rud (-64 do 64) żeby nie pętlować powietrza
                    int minY = Math.max(-64, cy - 40);
                    int maxY = Math.min(64, cy + 40);
                    
                    for (int x = cx - 40; x <= cx + 40; x++) {
                        for (int z = cz - 40; z <= cz + 40; z++) {
                            // Skanujemy TYLKO załadowane chunki, aby nie wywołać asynchronicznego ładowania świata i crashu
                            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                            
                            for (int y = minY; y <= maxY; y++) {
                                Material type = world.getType(x, y, z);
                                if (type == Material.DIAMOND_ORE || type == Material.DEEPSLATE_DIAMOND_ORE ||
                                    type == Material.GOLD_ORE || type == Material.DEEPSLATE_GOLD_ORE ||
                                    type == Material.IRON_ORE || type == Material.DEEPSLATE_IRON_ORE ||
                                    type == Material.ANCIENT_DEBRIS) {
                                    ores.add(new Location(world, x, y, z));
                                }
                            }
                        }
                    }
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (ores.isEmpty()) {
                            player.sendMessage(Component.text("Brak cennych rud w pobliskich załadowanych chunkach.", NamedTextColor.RED));
                            return;
                        }
                        player.sendMessage(Component.text("Znaleziono " + ores.size() + " rud! Zaznaczam przez 5s...", NamedTextColor.GREEN));
                        
                        List<org.bukkit.entity.BlockDisplay> markers = new ArrayList<>();
                        for (Location bLoc : ores) {
                            org.bukkit.entity.BlockDisplay stand = bLoc.getWorld().spawn(bLoc.clone(), org.bukkit.entity.BlockDisplay.class);
                            Material type = bLoc.getBlock().getType();
                            stand.setBlock(bLoc.getBlock().getBlockData());
                            stand.setGlowing(true);
                            stand.setTeleportDuration(0);
                            
                            if (type == Material.GOLD_ORE || type == Material.DEEPSLATE_GOLD_ORE) {
                                stand.setGlowColorOverride(org.bukkit.Color.YELLOW);
                            } else if (type == Material.DIAMOND_ORE || type == Material.DEEPSLATE_DIAMOND_ORE) {
                                stand.setGlowColorOverride(org.bukkit.Color.AQUA);
                            } else if (type == Material.IRON_ORE || type == Material.DEEPSLATE_IRON_ORE) {
                                stand.setGlowColorOverride(org.bukkit.Color.SILVER);
                            } else if (type == Material.ANCIENT_DEBRIS) {
                                stand.setGlowColorOverride(org.bukkit.Color.MAROON);
                            }
                            
                            markers.add(stand);
                        }
                        
                        new BukkitRunnable() {
                            int ticks = 0;
                            @Override
                            public void run() {
                                if (ticks >= 100 || !player.isOnline()) { // 5 sekund
                                    for (org.bukkit.entity.BlockDisplay stand : markers) {
                                        if (stand != null && !stand.isDead()) {
                                            stand.remove();
                                        }
                                    }
                                    if (player.isOnline()) {
                                        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.4f, 1.0f);
                                    }
                                    cancel();
                                    return;
                                }
                                ticks++;
                            }
                        }.runTaskTimer(plugin, 0L, 1L);
                    });
                }
            }.runTaskAsynchronously(plugin);
            
            cdGoldVein.put(player.getUniqueId(), now + 30000L);
        }
        
        // 6. Amulet Wiatru (Speed II + Jump Boost II, Aktywny)
        else if (id.equals("amulet_wind")) {
            event.setCancelled(true);
            if (now < cdWind.getOrDefault(player.getUniqueId(), 0L)) {
                return;
            }
            
            ItemStack boots = player.getInventory().getBoots();
            boolean hasHermes = "hermes_boots".equals(plugin.getCustomItemManager().getItemId(boots));
            
            if (!player.isInWater() && !hasHermes) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 2, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 160, 2, false, false, false));
                
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 15, 0.8, 0.5, 0.8, 0.05);
                player.getWorld().spawnParticle(Particle.SNOW_SHOVEL, player.getLocation(), 10, 0.3, 0.1, 0.3, 0.05);
                player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.5f, 1.3f);
                
                cdWind.put(player.getUniqueId(), now + 10000L); // 10s cooldown
            } else {
                player.sendMessage(Component.text("Nie możesz użyć tego amuletu w wodzie lub nosząc Buty Hermesa!", NamedTextColor.RED));
            }
        }
        
        // 7. Amulet Cienia (Invisibility z Dymem)
        else if (id.equals("amulet_shadow")) {
            event.setCancelled(true);
            if (now < cdShadow.getOrDefault(player.getUniqueId(), 0L)) {
                return;
            }
            
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, false, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false, false)); // Speed II
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
            
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks >= 60 || !player.isOnline()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.3f, 1.5f);
                        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
                        cancel();
                        return;
                    }
                    player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0.02);
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
            
            cdShadow.put(player.getUniqueId(), now + 25000L);
        }
        
        // 9. Amulet Nekromanty (Summon Zombie)
        else if (id.equals("amulet_necromancer")) {
            event.setCancelled(true);
            if (now < cdNecromancer.getOrDefault(player.getUniqueId(), 0L)) {
                return;
            }
            
            Location loc = player.getLocation();
            Zombie zombie = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
            zombie.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
            if (zombie.getAttribute(org.bukkit.attribute.Attribute.GENERIC_KNOCKBACK_RESISTANCE) != null) {
                zombie.getAttribute(org.bukkit.attribute.Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0); // Brak knockbacka
            }
            zombie.setHealth(20.0);
            zombie.setCustomName("§5Cień §7[§c8s§7]");
            zombie.setCustomNameVisible(true);
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false, false));
            
            // Dajemy mu hełm
            ItemStack helm = new ItemStack(Material.NETHERITE_HELMET);
            org.bukkit.inventory.meta.ItemMeta meta = helm.getItemMeta();
            meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, 5, true);
            helm.setItemMeta(meta);
            
            ItemStack sword = new ItemStack(Material.IRON_SWORD);
            org.bukkit.inventory.meta.ItemMeta swordMeta = sword.getItemMeta();
            swordMeta.addEnchant(org.bukkit.enchantments.Enchantment.DAMAGE_ALL, 5, true);
            sword.setItemMeta(swordMeta);
            
            if (zombie.getEquipment() != null) {
                zombie.getEquipment().setHelmet(helm);
                zombie.getEquipment().setHelmetDropChance(0.0f);
                zombie.getEquipment().setItemInMainHand(sword);
                zombie.getEquipment().setItemInMainHandDropChance(0.0f);
            }
            
            // Tagujemy zombie UUID ownera, zeby go nie atakowal
            zombie.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "owner"), org.bukkit.persistence.PersistentDataType.STRING, player.getUniqueId().toString());
            
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            player.getWorld().spawnParticle(Particle.ASH, loc.add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            player.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
            
            new BukkitRunnable() {
                int ticks = 32; // 8 seconds * 4 ticków/sec
                @Override
                public void run() {
                    if (ticks <= 0 || zombie.isDead()) {
                        if (!zombie.isDead()) {
                            Location zLoc = zombie.getLocation();
                            zLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, zLoc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                            zLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, zLoc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                            zLoc.getWorld().playSound(zLoc, Sound.ENTITY_WITHER_DEATH, 0.5f, 1.5f);
                            zombie.remove();
                        }
                        cancel();
                        return;
                    }
                    
                    if (ticks % 4 == 0) {
                        zombie.getWorld().spawnParticle(Particle.PORTAL, zombie.getLocation().add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0.05);
                        zombie.setCustomName("§5Cień §7[§c" + (ticks / 4) + "s§7]");
                    }
                    ticks--;
                    
                    // Sztuczne zmuszanie Zombiego do szybkiego ataku na inne potwory
                    LivingEntity currentTarget = zombie.getTarget();
                    if (currentTarget == null || currentTarget.isDead() || currentTarget == player || currentTarget.getLocation().distanceSquared(zombie.getLocation()) > 400.0) {
                        LivingEntity bestTarget = null;
                        double bestDist = 400.0; // max 20 kratek
                        for (Entity e : zombie.getNearbyEntities(20.0, 5.0, 20.0)) {
                            if (e != player && (e instanceof org.bukkit.entity.Monster || e instanceof Player)) {
                                double dist = e.getLocation().distanceSquared(zombie.getLocation());
                                if (dist < bestDist) {
                                    bestDist = dist;
                                    bestTarget = (LivingEntity) e;
                                }
                            }
                        }
                        if (bestTarget != null) {
                            zombie.setTarget(bestTarget);
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 5L); // Błyskawiczny czas reakcji co 0.25s
            
            cdNecromancer.put(player.getUniqueId(), now + 60000L); // 60s cooldown
        }
        
        // 10. Amulet Czasu (Zamrozenie / Slow)
        else if (id.equals("amulet_time")) {
            event.setCancelled(true);
            if (now < cdTime.getOrDefault(player.getUniqueId(), 0L)) {
                return;
            }
            
            player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation().add(0, 1, 0), 100, 3.0, 3.0, 3.0, 0.1);
            player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 50, 3.0, 3.0, 3.0, 0.1);
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.5f);
            
            List<LivingEntity> frozenMobs = new ArrayList<>();
            List<Player> frozenPlayers = new ArrayList<>();
            
            for (Entity e : player.getNearbyEntities(5.0, 5.0, 5.0)) {
                if (e instanceof LivingEntity le && le != player && e.getType() != EntityType.ARMOR_STAND) {
                    le.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.05);
                    
                    if (le instanceof Player p) {
                        frozenPlayers.add(p);
                        p.setWalkSpeed(0.0f); // calkowity zakaz ruchu po osi X i Z
                        p.setFlySpeed(0.0f);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 30, 250, false, false, false)); // zablokowanie skakania (Y)
                        p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                    } else {
                        frozenMobs.add(le);
                        if (le instanceof org.bukkit.entity.Mob mob) {
                            mob.setAI(false); // wyłączenie AI (zamarznięcie w bezruchu)
                        } else {
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 255, false, false, false));
                        }
                    }
                }
            }
            
            // Generowanie sfery cząsteczek promieniu 5
            Location center = player.getLocation().add(0, 1, 0);
            for (double t = 0; t <= 2 * Math.PI; t += Math.PI / 8) {
                for (double p = 0; p <= Math.PI; p += Math.PI / 8) {
                    double x = 5 * Math.sin(p) * Math.cos(t);
                    double y = 5 * Math.sin(p) * Math.sin(t);
                    double z = 5 * Math.cos(p);
                    center.getWorld().spawnParticle(Particle.SNOWFLAKE, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
                }
            }
            
            player.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f);
            player.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.3f);
            
            // Odmrozenie graczy po 1.5s (30 ticks)
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player p : frozenPlayers) {
                        if (p.isOnline()) {
                            p.setWalkSpeed(0.2f); // default 0.2
                            p.setFlySpeed(0.1f);  // default 0.1
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 2, false, false, false)); // Slowness III na 2s
                            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
                        }
                    }
                }
            }.runTaskLater(plugin, 30L);
            
            // Odmrozenie mobów po 3s (60 ticks)
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (LivingEntity le : frozenMobs) {
                        if (!le.isDead()) {
                            if (le instanceof org.bukkit.entity.Mob mob) {
                                mob.setAI(true); // powrot grawitacji i AI
                            }
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 2, false, false, false)); // Slowness III na 4s
                        }
                    }
                }
            }.runTaskLater(plugin, 60L);
            
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks >= 80 || !player.isOnline()) { // 4s = 80 ticks
                        cancel();
                        return;
                    }
                    if (ticks % 4 == 0) {
                        for (LivingEntity target : frozenMobs) {
                            if (!target.isDead()) {
                                target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.0);
                            }
                        }
                        for (Player target : frozenPlayers) {
                            if (target.isOnline()) {
                                target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.0);
                            }
                        }
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
            
            cdTime.put(player.getUniqueId(), now + 35000L);
        }
        
        // 12. Amulet Wulkanu (PPM - Fala ognia)
        else if (id.equals("amulet_volcano")) {
            event.setCancelled(true);
            if (now < cdVolcano.getOrDefault(player.getUniqueId(), 0L)) return;
            
            Location startLoc = player.getLocation();
            Vector dir = startLoc.getDirection().normalize().multiply(0.5);
            
            player.playSound(startLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
            player.playSound(startLoc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.5f);
            
            new BukkitRunnable() {
                int i = 0;
                Location current = startLoc.clone().add(0, 1, 0); // tułów
                List<UUID> hit = new ArrayList<>();
                @Override
                public void run() {
                    if (i >= 16 || !current.getBlock().isPassable()) { // 8 bloków = 16 kroków
                        cancel();
                        return;
                    }
                    current.add(dir);
                    current.getWorld().spawnParticle(Particle.FLAME, current, 5, 0.2, 0.2, 0.2, 0.02);
                    current.getWorld().spawnParticle(Particle.LAVA, current, 2, 0.2, 0.2, 0.2, 0.02);
                    
                    for (Entity e : current.getWorld().getNearbyEntities(current, 1.5, 1.5, 1.5)) {
                        if (e instanceof LivingEntity le && le != player && !hit.contains(le.getUniqueId())) {
                            if (le instanceof org.bukkit.entity.Monster || le instanceof Player) {
                                le.setFireTicks(80); // 4s
                                le.damage(3.0, player);
                                hit.add(le.getUniqueId());
                            }
                        }
                    }
                    i++;
                }
            }.runTaskTimer(plugin, 0L, 1L); // Szybki pocisk
            
            cdVolcano.put(player.getUniqueId(), now + 35000L);
        }

        // 13. Amulet Iluzji (PPM - Kopie)
        else if (id.equals("amulet_illusion")) {
            event.setCancelled(true);
            if (now < cdIllusion.getOrDefault(player.getUniqueId(), 0L)) return;
            
            Location pLoc = player.getLocation();
            player.playSound(pLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            
            List<org.bukkit.entity.ArmorStand> copies = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                double rx = (random.nextDouble() - 0.5) * 10;
                double rz = (random.nextDouble() - 0.5) * 10;
                Location spawn = pLoc.clone().add(rx, 0, rz);
                // Usunięto teleportację na najwyższy blok (getHighestBlockYAt), aby działało również pod ziemią
                
                org.bukkit.entity.ArmorStand as = (org.bukkit.entity.ArmorStand) spawn.getWorld().spawnEntity(spawn, EntityType.ARMOR_STAND);
                as.setBasePlate(false);
                as.setArms(true);
                as.setInvulnerable(true);
                for (org.bukkit.inventory.EquipmentSlot slot : new org.bukkit.inventory.EquipmentSlot[]{
                        org.bukkit.inventory.EquipmentSlot.HAND, org.bukkit.inventory.EquipmentSlot.OFF_HAND,
                        org.bukkit.inventory.EquipmentSlot.FEET, org.bukkit.inventory.EquipmentSlot.LEGS,
                        org.bukkit.inventory.EquipmentSlot.CHEST, org.bukkit.inventory.EquipmentSlot.HEAD}) {
                    as.addEquipmentLock(slot, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
                    as.addEquipmentLock(slot, org.bukkit.entity.ArmorStand.LockType.REMOVING_OR_CHANGING);
                }
                
                if (as.getEquipment() != null && player.getEquipment() != null) {
                    as.getEquipment().setArmorContents(player.getEquipment().getArmorContents());
                    as.getEquipment().setItemInMainHand(player.getInventory().getItemInMainHand());
                }
                
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(player);
                    head.setItemMeta(meta);
                }
                if (as.getEquipment() != null) {
                    as.getEquipment().setHelmet(head);
                }
                
                copies.add(as);
                player.playSound(spawn, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            }
            
            activeIllusions.put(player.getUniqueId(), now + 5000L); // 5s of player invisibility to mobs
            
            // Wyczyść aktualne cele (jeśli moby atakowały gracza)
            for (Entity e : pLoc.getWorld().getNearbyEntities(pLoc, 15.0, 15.0, 15.0)) {
                if (e instanceof org.bukkit.entity.Mob mob) {
                    if (mob.getTarget() == player) {
                        mob.setTarget(null);
                    }
                }
            }
            
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks >= 100 || !player.isOnline()) { // 5s
                        for (org.bukkit.entity.ArmorStand as : copies) {
                            if (!as.isDead()) {
                                as.getWorld().spawnParticle(Particle.SMOKE_LARGE, as.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
                                as.getWorld().playSound(as.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
                                as.remove();
                            }
                        }
                        cancel();
                        return;
                    }
                    
                    // Zmuszamy moby do ciągłego patrzenia/skupienia na klonach (co 10 ticków)
                    if (ticks % 10 == 0) {
                        for (Entity e : player.getNearbyEntities(15.0, 15.0, 15.0)) {
                            if (e instanceof org.bukkit.entity.Mob mob) {
                                // Znajdź najbliższego klona
                                org.bukkit.entity.ArmorStand bestCopy = null;
                                double bestDist = 400.0;
                                for (org.bukkit.entity.ArmorStand as : copies) {
                                    if (!as.isDead()) {
                                        double d = as.getLocation().distanceSquared(mob.getLocation());
                                        if (d < bestDist) {
                                            bestDist = d;
                                            bestCopy = as;
                                        }
                                    }
                                }
                                if (bestCopy != null) {
                                    mob.setTarget(bestCopy);
                                }
                            }
                        }
                    }
                    
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
            
            cdIllusion.put(player.getUniqueId(), now + 35000L);
        }

        // 14. Amulet Tsunami (PPM - Fala przesuwająca)
        else if (id.equals("amulet_tsunami")) {
            event.setCancelled(true);
            if (now < cdTsunami.getOrDefault(player.getUniqueId(), 0L)) return;
            
            Location startLoc = player.getLocation();
            Vector dir = startLoc.getDirection().setY(0).normalize(); // Pozioma fala
            
            player.playSound(startLoc, Sound.ENTITY_GENERIC_SPLASH, 1.0f, 0.5f);
            player.playSound(startLoc, Sound.BLOCK_WATER_AMBIENT, 1.0f, 0.5f);
            
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks >= 40 || !player.isOnline()) { // 2s (40 ticków)
                        cancel();
                        return;
                    }
                    
                    double distance = (ticks / 40.0) * 8.0; // max 8 bloków
                    Location waveCenter = player.getLocation().add(dir.clone().multiply(distance));
                    
                    Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                    for (double i = -2.0; i <= 2.0; i += 0.5) {
                        Location pLoc = waveCenter.clone().add(right.clone().multiply(i)).add(0, 1, 0);
                        pLoc.getWorld().spawnParticle(Particle.DRIP_WATER, pLoc, 5, 0.2, 0.5, 0.2, 0.1);
                        pLoc.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, pLoc, 2, 0.2, 0.5, 0.2, 0.1);
                    }
                    
                    for (Entity e : waveCenter.getWorld().getNearbyEntities(waveCenter, 3.0, 2.0, 3.0)) {
                        if (e instanceof LivingEntity le && le != player) {
                            if (le instanceof org.bukkit.entity.Monster || le instanceof Player) {
                                Vector push = dir.clone().multiply(0.4).setY(0.1);
                                le.setVelocity(le.getVelocity().add(push));
                            }
                        }
                    }
                    
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
            
            cdTsunami.put(player.getUniqueId(), now + 60000L);
        }
    }

    // Ochrona przed atakiem Zombiego
    @EventHandler
    public void onSummonTarget(org.bukkit.event.entity.EntityTargetEvent event) {
        if (event.getEntity() instanceof Zombie zombie) {
            String ownerStr = zombie.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "owner"), org.bukkit.persistence.PersistentDataType.STRING);
            if (ownerStr != null) {
                if (event.getTarget() != null) {
                    if (event.getTarget().getUniqueId().toString().equals(ownerStr)) {
                        event.setCancelled(true); // Nie atakuj właściciela
                    } else if (!(event.getTarget() instanceof org.bukkit.entity.Monster) && !(event.getTarget() instanceof Player)) {
                        event.setCancelled(true); // Atakuj tylko wrogie moby i graczy
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSummonDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Zombie zombie) {
            String ownerStr = zombie.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "owner"), org.bukkit.persistence.PersistentDataType.STRING);
            if (ownerStr != null && event.getEntity().getUniqueId().toString().equals(ownerStr)) {
                event.setCancelled(true); // Zapobiega zadaniu obrażeń właścicielowi przez AoE/lagi
            }
        }
    }

    // Blokada targetowania gracza podczas trwania Amuletu Iluzji
    @EventHandler
    public void onEntityTargetPlayer(org.bukkit.event.entity.EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player) {
            if (System.currentTimeMillis() < activeIllusions.getOrDefault(player.getUniqueId(), 0L)) {
                event.setCancelled(true);
            }
        }
    }
}
