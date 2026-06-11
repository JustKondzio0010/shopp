package pl.lukas.reputation.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import java.util.List;

public class WorldGuardHook {
    private static boolean initialized = false;
    private static boolean available = false;

    private static void init() {
        if (initialized) return;
        try {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
                Class.forName("com.sk89q.worldguard.WorldGuard");
                Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                available = true;
            }
        } catch (ClassNotFoundException e) {
            available = false;
        }
        initialized = true;
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    public static boolean isInRegion(Location loc, List<String> regionIds) {
        if (!isAvailable() || regionIds == null || regionIds.isEmpty()) {
            return false;
        }
        try {
            // Get WorldGuard instance: WorldGuard.getInstance()
            Object wgInstance = Class.forName("com.sk89q.worldguard.WorldGuard")
                    .getMethod("getInstance")
                    .invoke(null);

            // wgInstance.getPlatform()
            Object platform = wgInstance.getClass().getMethod("getPlatform").invoke(wgInstance);

            // platform.getRegionContainer()
            Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            // BukkitAdapter.adapt(loc)
            Object adaptedLoc = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter")
                    .getMethod("adapt", Location.class)
                    .invoke(null, loc);

            // regionContainer.createQuery()
            Object query = regionContainer.getClass().getMethod("createQuery").invoke(regionContainer);

            // query.getApplicableRegions(adaptedLoc)
            Object regionSet = query.getClass()
                    .getMethod("getApplicableRegions", Class.forName("com.sk89q.worldedit.util.Location"))
                    .invoke(query, adaptedLoc);

            // regionSet is an Iterable<ProtectedRegion>
            Iterable<?> regions = (Iterable<?>) regionSet;
            for (Object region : regions) {
                // region.getId()
                String id = (String) region.getClass().getMethod("getId").invoke(region);
                if (id != null) {
                    for (String disabledRegion : regionIds) {
                        if (id.equalsIgnoreCase(disabledRegion)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Catch exceptions to avoid issues with different WorldGuard API versions
        }
        return false;
    }
}
