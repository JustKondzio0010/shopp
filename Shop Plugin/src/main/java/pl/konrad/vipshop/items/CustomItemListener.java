package pl.konrad.vipshop.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pl.konrad.vipshop.VipShop;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomItemListener implements Listener {

    private final VipShop plugin;
    private final Random random = new Random();

    // Map to track the last clicked block face for players (needed for area mining calculations)
    private final Map<UUID, BlockFace> lastClickedFace = new ConcurrentHashMap<>();
    
    // Set to prevent infinite loops when triggering mock BlockBreakEvents
    private final Set<UUID> bypassBreakEvent = Collections.synchronizedSet(new HashSet<>());
    
    // Set to prevent infinite loops when triggering mock EntityDamageByEntityEvents (Reaper Scythe)
    private final Set<UUID> bypassDamageEvent = Collections.synchronizedSet(new HashSet<>());

    public CustomItemListener(VipShop plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // 1. Tool Block Break Abilities (Hammers, Drills)
    // ==========================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeftClickBlock(PlayerInteractEvent event) {
        if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            if (event.getBlockFace() != null) {
                lastClickedFace.put(event.getPlayer().getUniqueId(), event.getBlockFace());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (bypassBreakEvent.contains(uuid)) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        String itemId = plugin.getCustomItemManager().getItemId(tool);
        if (itemId == null) {
            return;
        }

        Block block = event.getBlock();
        
        // Handle Hammer/Excavator Area Mining
        if (itemId.equals("miner_pickaxe") || itemId.equals("compact_hammer") || itemId.equals("tunnel_destroyer") || itemId.equals("titan_excavator")) {
            BlockFace face = lastClickedFace.getOrDefault(uuid, BlockFace.UP);
            
            if (itemId.equals("miner_pickaxe")) {
                // 1x2 Vertical tunnel
                int blockY = block.getY();
                int eyeY = player.getEyeLocation().getBlockY();
                int otherY = (blockY >= eyeY) ? blockY - 1 : blockY + 1;
                Block other = block.getWorld().getBlockAt(block.getX(), otherY, block.getZ());
                breakSingleBlock(player, other, tool);
            } else {
                int radius = itemId.equals("compact_hammer") ? 1 : (itemId.equals("tunnel_destroyer") ? 1 : 2);
                boolean isCentered = !itemId.equals("compact_hammer") && !itemId.equals("titan_excavator"); // 2x2 and 4x4 are offset
                
                List<Block> blocks = getAreaBlocks(block, face, radius, isCentered);
                
                if (itemId.equals("titan_excavator")) {
                    // Stagger 5x5 mining over ticks to prevent lag spikes
                    new BukkitRunnable() {
                        int index = 0;
                        @Override
                        public void run() {
                            if (index >= blocks.size() || !player.isOnline()) {
                                cancel();
                                return;
                            }
                            bypassBreakEvent.add(uuid);
                            for (int i = 0; i < 5 && index < blocks.size(); i++) {
                                Block target = blocks.get(index++);
                                breakSingleBlock(player, target, tool);
                            }
                            bypassBreakEvent.remove(uuid);
                        }
                    }.runTaskTimer(plugin, 0L, 1L);
                } else {
                    // Break 2x2/3x3 instantly
                    bypassBreakEvent.add(uuid);
                    for (Block target : blocks) {
                        breakSingleBlock(player, target, tool);
                    }
                    bypassBreakEvent.remove(uuid);
                }
            }
        }
        
        // Handle Auto-Smelt Drill
        else if (itemId.equals("smelt_drill")) {
            Material type = block.getType();
            Material smelted = getSmeltedResult(type);
            if (smelted != null) {
                event.setDropItems(false);
                
                int fortune = tool.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
                int amount = 1;
                if (fortune > 0) {
                    amount = 1 + random.nextInt(fortune + 1);
                }
                if (type == Material.COPPER_ORE || type == Material.DEEPSLATE_COPPER_ORE) {
                    amount *= (2 + random.nextInt(4)); // Copper naturally drops 2-5 raw copper
                }

                ItemStack drops = new ItemStack(smelted, amount);
                block.getWorld().dropItemNaturally(block.getLocation(), drops);
                block.getWorld().spawnParticle(Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }

    private void breakSingleBlock(Player player, Block block, ItemStack tool) {
        if (block.getType().isAir() || block.getType() == Material.BEDROCK || block.getType() == Material.BARRIER) {
            return;
        }
        block.breakNaturally(tool);
    }

    private List<Block> getAreaBlocks(Block start, BlockFace face, int radius, boolean centered) {
        List<Block> blocks = new ArrayList<>();
        World world = start.getWorld();
        int sx = start.getX();
        int sy = start.getY();
        int sz = start.getZ();

        if (centered) {
            // Centered loops (3x3, 5x5)
            if (face == BlockFace.UP || face == BlockFace.DOWN) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x == 0 && z == 0) continue;
                        blocks.add(world.getBlockAt(sx + x, sy, sz + z));
                    }
                }
            } else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        if (x == 0 && y == 0) continue;
                        blocks.add(world.getBlockAt(sx + x, sy + y, sz));
                    }
                }
            } else { // EAST / WEST
                for (int z = -radius; z <= radius; z++) {
                    for (int y = -radius; y <= radius; y++) {
                        if (z == 0 && y == 0) continue;
                        blocks.add(world.getBlockAt(sx, sy + y, sz + z));
                    }
                }
            }
        } else if (radius == 1) {
            // Offset loops (2x2)
            if (face == BlockFace.UP || face == BlockFace.DOWN) {
                blocks.add(world.getBlockAt(sx + 1, sy, sz));
                blocks.add(world.getBlockAt(sx, sy, sz + 1));
                blocks.add(world.getBlockAt(sx + 1, sy, sz + 1));
            } else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                blocks.add(world.getBlockAt(sx + 1, sy, sz));
                blocks.add(world.getBlockAt(sx, sy - 1, sz));
                blocks.add(world.getBlockAt(sx + 1, sy - 1, sz));
            } else { // EAST / WEST
                blocks.add(world.getBlockAt(sx, sy, sz + 1));
                blocks.add(world.getBlockAt(sx, sy - 1, sz));
                blocks.add(world.getBlockAt(sx, sy - 1, sz + 1));
            }
        } else if (radius == 2) {
            // Offset loops (4x4)
            if (face == BlockFace.UP || face == BlockFace.DOWN) {
                for (int x = -1; x <= 2; x++) {
                    for (int z = -1; z <= 2; z++) {
                        if (x == 0 && z == 0) continue;
                        blocks.add(world.getBlockAt(sx + x, sy, sz + z));
                    }
                }
            } else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                for (int x = -1; x <= 2; x++) {
                    for (int y = -1; y <= 2; y++) {
                        if (x == 0 && y == 0) continue;
                        blocks.add(world.getBlockAt(sx + x, sy + y, sz));
                    }
                }
            } else { // EAST / WEST
                for (int z = -1; z <= 2; z++) {
                    for (int y = -1; y <= 2; y++) {
                        if (z == 0 && y == 0) continue;
                        blocks.add(world.getBlockAt(sx, sy + y, sz + z));
                    }
                }
            }
        }
        return blocks;
    }

    private Material getSmeltedResult(Material raw) {
        return switch (raw) {
            case IRON_ORE, DEEPSLATE_IRON_ORE, RAW_IRON_BLOCK -> Material.IRON_INGOT;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, RAW_GOLD_BLOCK -> Material.GOLD_INGOT;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE, RAW_COPPER_BLOCK -> Material.COPPER_INGOT;
            default -> null;
        };
    }

    // ==========================================
    // 2. Weapon Combat Abilities (Lifesteal, AoE)
    // ==========================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeaponDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        String itemId = plugin.getCustomItemManager().getItemId(weapon);
        if (itemId == null) {
            return;
        }

        if (bypassDamageEvent.contains(player.getUniqueId())) {
            return;
        }

        Entity target = event.getEntity();

        // Vampire Dagger Lifesteal (+1 Heart / 2 HP)
        if (itemId.equals("vampire_dagger")) {
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            double currentHealth = player.getHealth();
            player.setHealth(Math.min(maxHealth, currentHealth + 2.0));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
            player.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.05);
        }

        // Reaper Scythe AoE Sweep damage to all mobs in 3-block radius
        else if (itemId.equals("reaper_scythe")) {
            double damage = event.getDamage();
            bypassDamageEvent.add(player.getUniqueId());
            for (Entity entity : target.getNearbyEntities(3.0, 3.0, 3.0)) {
                if (entity instanceof LivingEntity living && entity != player && entity != target) {
                    if (entity.getType() != EntityType.ARMOR_STAND) {
                        living.damage(damage * 0.75, player); // 75% of primary damage to sweep targets
                        living.getWorld().spawnParticle(Particle.SWEEP_ATTACK, living.getLocation().add(0, 1, 0), 1);
                    }
                }
            }
            bypassDamageEvent.remove(player.getUniqueId());
        }
    }

    // ==========================================
    // 3. Headhunter Sword Mob Drop
    // ==========================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        String itemId = plugin.getCustomItemManager().getItemId(weapon);
        if (itemId == null || !itemId.equals("headhunter_sword")) {
            return;
        }

        // 5% chance to drop head
        if (random.nextDouble() < 0.05) {
            Material head = getMobHead(entity.getType());
            if (head != null) {
                event.getDrops().add(new ItemStack(head));
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            }
        }
    }

    private Material getMobHead(EntityType type) {
        return switch (type) {
            case CREEPER -> Material.CREEPER_HEAD;
            case SKELETON -> Material.SKELETON_SKULL;
            case WITHER_SKELETON -> Material.WITHER_SKELETON_SKULL;
            case ZOMBIE -> Material.ZOMBIE_HEAD;
            case PIGLIN -> Material.PIGLIN_HEAD;
            default -> null;
        };
    }

    // ==========================================
    // 4. Custom Armor Perks (NightVision, Speed, FireResist)
    // ==========================================

    @EventHandler
    public void onPlayerJoinArmor(PlayerJoinEvent event) {
        scheduleArmorUpdate(event.getPlayer());
    }

    @EventHandler
    public void onInventoryCloseArmor(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            scheduleArmorUpdate(player);
        }
    }

    @EventHandler
    public void onInteractArmor(PlayerInteractEvent event) {
        // Equipping armor via right click
        ItemStack item = event.getItem();
        if (item != null && item.getType().name().contains("HELMET") || item != null && item.getType().name().contains("CHESTPLATE") || item != null && item.getType().name().contains("BOOTS")) {
            scheduleArmorUpdate(event.getPlayer());
        }
    }

    @EventHandler
    public void onDispenseArmor(BlockDispenseArmorEvent event) {
        if (event.getTargetEntity() instanceof Player player) {
            scheduleArmorUpdate(player);
        }
    }

    @EventHandler
    public void onItemBreakArmor(PlayerItemBreakEvent event) {
        scheduleArmorUpdate(event.getPlayer());
    }

    @EventHandler
    public void onRespawnArmor(PlayerRespawnEvent event) {
        scheduleArmorUpdate(event.getPlayer());
    }

    private void scheduleArmorUpdate(Player player) {
        // Delay by 1 tick to let inventory update complete
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    updateArmorEffects(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    private void updateArmorEffects(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chest = player.getInventory().getChestplate();
        ItemStack boots = player.getInventory().getBoots();

        // Night Vision Helmet
        String helmId = plugin.getCustomItemManager().getItemId(helmet);
        if (helmId != null && helmId.equals("nightvision_helmet")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
        } else {
            PotionEffect active = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
            if (active != null && active.getDuration() > 100000) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
        }

        // Magma Chestplate (Fire Resistance)
        String chestId = plugin.getCustomItemManager().getItemId(chest);
        if (chestId != null && chestId.equals("magma_chestplate")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false, false));
        } else {
            PotionEffect active = player.getPotionEffect(PotionEffectType.FIRE_RESISTANCE);
            if (active != null && active.getDuration() > 100000) {
                player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
            }
        }

        // Hermes Boots (Speed II)
        String bootsId = plugin.getCustomItemManager().getItemId(boots);
        if (bootsId != null && bootsId.equals("hermes_boots")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false, false));
        } else {
            PotionEffect active = player.getPotionEffect(PotionEffectType.SPEED);
            if (active != null && active.getDuration() > 100000) {
                player.removePotionEffect(PotionEffectType.SPEED);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack boots = player.getInventory().getBoots();
        String bootsId = plugin.getCustomItemManager().getItemId(boots);
        if (bootsId != null && bootsId.equals("hermes_boots")) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.4f, 1.5f);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireDamage(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.FIRE && cause != EntityDamageEvent.DamageCause.FIRE_TICK && cause != EntityDamageEvent.DamageCause.LAVA && cause != EntityDamageEvent.DamageCause.HOT_FLOOR) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack chest = player.getInventory().getChestplate();
        String chestId = plugin.getCustomItemManager().getItemId(chest);
        if (chestId != null && chestId.equals("magma_chestplate")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArtifactDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Artefakt Chciwości curse: +30% damage taken while holding it
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        boolean holdingArtifact = "greed_artifact".equals(plugin.getCustomItemManager().getItemId(mainHand)) ||
                                  "greed_artifact".equals(plugin.getCustomItemManager().getItemId(offHand));

        if (holdingArtifact) {
            event.setDamage(event.getDamage() * 1.30);
            
            // Re-apply Mining Fatigue I just in case it was cleared
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 100, 0, false, false, false));
        }
    }

    @EventHandler
    public void onArtifactHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack nextItem = player.getInventory().getItem(event.getNewSlot());
        String nextId = plugin.getCustomItemManager().getItemId(nextItem);
        
        boolean needsFatigue = "greed_artifact".equals(nextId) || "tunnel_destroyer".equals(nextId) || "titan_excavator".equals(nextId);

        if (needsFatigue) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, Integer.MAX_VALUE, 0, false, false, false));
        } else {
            // Check offhand too before removing
            ItemStack offHand = player.getInventory().getItemInOffHand();
            String offId = plugin.getCustomItemManager().getItemId(offHand);
            boolean offHandNeedsFatigue = "greed_artifact".equals(offId) || "tunnel_destroyer".equals(offId) || "titan_excavator".equals(offId);
            
            if (!offHandNeedsFatigue) {
                PotionEffect active = player.getPotionEffect(PotionEffectType.SLOW_DIGGING);
                if (active != null && active.getDuration() > 100000) {
                    player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
                }
            }
        }
    }

    // ==========================================
    // 5. TreeFeller Axe Ability
    // ==========================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTreeChop(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        String id = plugin.getCustomItemManager().getItemId(tool);
        
        if (id == null || !id.equals("lumberjack_axe")) {
            return;
        }

        Block start = event.getBlock();
        if (!isLog(start.getType())) {
            return;
        }

        // BFS traversal to gather up to 150 connected logs and leaves
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();
        List<Block> toBreak = new ArrayList<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && toBreak.size() < 150) {
            Block current = queue.poll();
            if (current != start) {
                toBreak.add(current);
            }

            // Look in 26 adjacent directions (including diagonals)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Block adj = current.getRelative(dx, dy, dz);
                        if (!visited.contains(adj) && (isLog(adj.getType()) || isLeaves(adj.getType()))) {
                            visited.add(adj);
                            queue.add(adj);
                        }
                    }
                }
            }
        }

        // Break blocks on main thread since 150 is small, but bypassing listeners
        bypassBreakEvent.add(player.getUniqueId());
        for (Block log : toBreak) {
            log.breakNaturally(tool);
        }
        bypassBreakEvent.remove(player.getUniqueId());
    }

    private boolean isLog(Material mat) {
        String name = mat.name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.equals("MANGROVE_ROOTS");
    }

    private boolean isLeaves(Material mat) {
        return mat.name().endsWith("_LEAVES");
    }

    // ==========================================
    // 6. Motyka Obfitości (3x3 Crop Harvest & Replant)
    // ==========================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        String id = plugin.getCustomItemManager().getItemId(tool);
        
        if (id == null || !id.equals("harvest_hoe")) {
            return;
        }

        Block center = event.getClickedBlock();
        if (center == null) {
            return;
        }

        if (center.getType() != Material.FARMLAND && !(center.getBlockData() instanceof Ageable)) {
            return;
        }

        event.setCancelled(true);

        World world = center.getWorld();
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        // 3x3 crop harvesting
        boolean harvestedAny = false;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                // Determine target crop block (either the clicked block or the one above farmland)
                Block target = world.getBlockAt(cx + x, cy, cz + z);
                if (target.getType() == Material.FARMLAND) {
                    target = world.getBlockAt(cx + x, cy + 1, cz + z);
                }

                if (target.getBlockData() instanceof Ageable ageable) {
                    if (ageable.getAge() == ageable.getMaximumAge()) {
                        // Gather crops and drop naturally
                        Collection<ItemStack> drops = target.getDrops(tool);
                        for (ItemStack drop : drops) {
                            world.dropItemNaturally(target.getLocation(), drop);
                        }

                        // Replant by setting crop age back to 0
                        ageable.setAge(0);
                        target.setBlockData(ageable);
                        harvestedAny = true;
                    }
                }
            }
        }

        if (harvestedAny) {
            world.playSound(center.getLocation(), Sound.BLOCK_CROP_BREAK, 1.0f, 1.0f);
            world.spawnParticle(Particle.VILLAGER_HAPPY, center.getLocation().add(0.5, 1.0, 0.5), 10, 0.5, 0.2, 0.5, 0.05);
        }
    }

    // ==========================================
    // 7. Magnetyczna Wędka (Fishing time optimizer)
    // ==========================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.FISHING) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();
        String id = plugin.getCustomItemManager().getItemId(rod);
        
        if (id != null && id.equals("magnetic_rod")) {
            org.bukkit.entity.FishHook hook = event.getHook();
            // Fast fish bite: set wait time between 20 ticks (1s) and 60 ticks (3s)
            hook.setMinWaitTime(20);
            hook.setMaxWaitTime(60);
        }
    }

    // ==========================================
    // 8. Bottomless Backpack (Save on Inventory Close)
    // ==========================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onBackpackInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        String id = plugin.getCustomItemManager().getItemId(item);
        
        if (id == null || !id.equals("bottomless_backpack")) {
            return;
        }

        event.setCancelled(true);
        player.closeInventory();

        String backpackUuid = plugin.getCustomItemManager().getBackpackUuid(item);
        if (backpackUuid == null) {
            // Recreate item just to patch missing NBT tags
            ItemStack correct = plugin.getCustomItemManager().createItem("bottomless_backpack");
            player.getInventory().setItemInMainHand(correct);
            backpackUuid = plugin.getCustomItemManager().getBackpackUuid(correct);
        }

        openBackpack(player, backpackUuid);
    }

    private void openBackpack(Player player, String uuid) {
        Inventory inv = Bukkit.createInventory(new BackpackHolder(uuid), 54, Component.text("Plecak Bez Dna"));
        
        // Load contents from SQLite database
        String data = plugin.getDatabaseManager().getBackpackContents(uuid);
        if (data != null) {
            try {
                ItemStack[] items = plugin.getCustomItemManager().fromBase64(data);
                inv.setContents(items);
            } catch (IOException e) {
                player.sendMessage("§cBłąd podczas wczytywania zawartości plecaka!");
            }
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.0f);
    }

    @EventHandler
    public void onBackpackClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BackpackHolder holder)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        String uuid = holder.uuid;
        ItemStack[] contents = event.getInventory().getContents();

        // Save asynchronously to prevent SQLite disk locking the main tick
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String base64 = plugin.getCustomItemManager().toBase64(contents);
                    plugin.getDatabaseManager().saveBackpackContents(uuid, base64);
                } catch (Exception e) {
                    plugin.getLogger().severe("Blad podczas zapisu plecaka: " + uuid);
                }
            }
        }.runTaskAsynchronously(plugin);

        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8f, 0.8f);
    }

    public static final class BackpackHolder implements InventoryHolder {
        public final String uuid;

        public BackpackHolder(String uuid) {
            this.uuid = uuid;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return null;
        }
    }
}
