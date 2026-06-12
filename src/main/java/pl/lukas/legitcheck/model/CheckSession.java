package pl.lukas.legitcheck.model;

import org.bukkit.Location;
import java.util.UUID;

public class CheckSession {
    private final UUID playerUuid;
    private final String playerName;
    private final UUID adminUuid;
    private final String adminName;
    private final Location originalLocation;
    private final Location originalAdminLocation;
    private final long startTime;

    public CheckSession(UUID playerUuid, String playerName, UUID adminUuid, String adminName, Location originalLocation, Location originalAdminLocation) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.adminUuid = adminUuid;
        this.adminName = adminName;
        this.originalLocation = originalLocation;
        this.originalAdminLocation = originalAdminLocation;
        this.startTime = System.currentTimeMillis();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getAdminUuid() {
        return adminUuid;
    }

    public String getAdminName() {
        return adminName;
    }

    public Location getOriginalLocation() {
        return originalLocation;
    }

    public Location getOriginalAdminLocation() {
        return originalAdminLocation;
    }

    public long getStartTime() {
        return startTime;
    }
}
