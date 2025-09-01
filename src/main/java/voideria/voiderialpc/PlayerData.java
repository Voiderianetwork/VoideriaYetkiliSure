package voideria.voiderialpc;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String playerName;
    private long totalSeconds;
    private final Set<String> rewards = new HashSet<>();

    public PlayerData(UUID uuid, String playerName, long totalSeconds) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.totalSeconds = totalSeconds;
    }

    public void addSeconds(long seconds) {
        this.totalSeconds += seconds;
    }

    public void resetTime() {
        this.totalSeconds = 0;
        rewards.clear();
    }

    public void addReward(String reward) {
        rewards.add(reward);
    }

    public boolean hasReward(String reward) {
        return rewards.contains(reward);
    }

    public String getFormattedTime() {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        return String.format("%d gun %d saat %d dakika %d saniye", days, hours, minutes, seconds);
    }

    public String getPlayerName() { 
        if (playerName == null || playerName.isEmpty() || playerName.equals("Unknown")) {
            updatePlayerName();
        }
        return playerName; 
    }
    
    private void updatePlayerName() {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.getName() != null) {
            playerName = offlinePlayer.getName();
        }
    }
    
    public long getTotalSeconds() { return totalSeconds; }
    public UUID getUuid() { return uuid; } // EKLENDİ
    public Set<String> getRewards() { return Collections.unmodifiableSet(rewards); }
}