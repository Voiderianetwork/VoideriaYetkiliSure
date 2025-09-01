package voideria.voiderialpc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.regex.Pattern;

public class RankManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final Map<String, RankConfig> ranks = new HashMap<>();

    public RankManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        loadRanks();
    }

    public void loadRanks() {
        ranks.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ranks");
        if (section != null) {
            for (String rank : section.getKeys(false)) {
                String permission = section.getString(rank + ".permission");
                int requiredHours = section.getInt(rank + ".required-hours");
                String rewardCommand = section.getString(rank + ".reward-command");
                String rewardMessage = section.getString(rank + ".reward-message", 
                    "&aTebrikler! {rank} rutbesi icin odulunuzu kazandiniz.");
                ranks.put(rank, new RankConfig(permission, requiredHours, rewardCommand, rewardMessage));
            }
        }
    }

    // Online ve offline tüm oyuncular için ödül kontrolü
    public void checkPlayerRewards(OfflinePlayer player) {
        PlayerData data = dataManager.getOrCreatePlayerData(player.getUniqueId(), player.getName());
        
        Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
            for (Map.Entry<String, RankConfig> entry : ranks.entrySet()) {
                String rankName = entry.getKey();
                RankConfig rankConfig = entry.getValue();
                
                long requiredSeconds = rankConfig.getRequiredHours() * 3600;
                
                // Süre yeterliyse ve ödül alınmamışsa
                if (data.getTotalSeconds() >= requiredSeconds && !data.hasReward(rankName)) {
                    executeReward(rankConfig.getRewardCommand(), player);
                    
                    // Oyuncu online ise mesaj gönder
                    if (player.isOnline() && player.getPlayer() != null) {
                        String message = ChatColor.translateAlternateColorCodes('&', 
                            rankConfig.getRewardMessage().replace("{rank}", rankName));
                        player.getPlayer().sendMessage(message);
                    }
                    
                    data.addReward(rankName);
                    dataManager.savePlayerData(player.getUniqueId());
                }
            }
        });
    }

    public void grantRank(OfflinePlayer player, String rank) {
        Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
            RankConfig config = ranks.get(rank);
            if (config != null) {
                dataManager.getOrCreatePlayerData(player.getUniqueId(), player.getName());
                
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(), 
                    "lp user " + player.getName() + " permission set " + config.getPermission() + " true"
                );
                
                if (player.isOnline()) {
                    player.getPlayer().sendMessage(ChatColor.GREEN + rank + " rutbesi verildi!");
                }
            }
        });
    }

    public void revokeAllRanks(OfflinePlayer player) {
        Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
            for (RankConfig config : ranks.values()) {
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(), 
                    "lp user " + player.getName() + " permission unset " + config.getPermission()
                );
            }
            
            dataManager.removePlayerData(player.getUniqueId());
        });
    }

    // Online ve offline oyunculara süre ekleme
    public void addTime(UUID uuid, int minutes, String playerName) {
        PlayerData data = dataManager.getOrCreatePlayerData(uuid, playerName);
        data.addSeconds(minutes * 60);
        dataManager.savePlayerData(uuid);
        
        // Ödül kontrolü yap
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        checkPlayerRewards(player);
    }

    // Online ve offline oyunculara süre çıkarma
    public void removeTime(UUID uuid, int minutes, String playerName) {
        PlayerData data = dataManager.getOrCreatePlayerData(uuid, playerName);
        long secondsToRemove = minutes * 60;
        if (data.getTotalSeconds() > secondsToRemove) {
            data.addSeconds(-secondsToRemove);
        } else {
            data.resetTime();
        }
        dataManager.savePlayerData(uuid);
    }

    // Ödül komutunu çalıştır (online/offline farketmez)
    private void executeReward(String command, OfflinePlayer player) {
        String safePlayerName = player.getName() != null ? 
            Pattern.compile("[^a-zA-Z0-9_]").matcher(player.getName()).replaceAll("") : 
            "UnknownPlayer";
            
        String formatted = command.replace("{player}", safePlayerName);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted);
    }

    public Map<String, RankConfig> getRanks() {
        return Collections.unmodifiableMap(ranks);
    }
}

class RankConfig {
    private final String permission;
    private final int requiredHours;
    private final String rewardCommand;
    private final String rewardMessage;

    public RankConfig(String permission, int requiredHours, String rewardCommand, String rewardMessage) {
        this.permission = permission;
        this.requiredHours = requiredHours;
        this.rewardCommand = rewardCommand;
        this.rewardMessage = rewardMessage;
    }

    public String getPermission() { return permission; }
    public int getRequiredHours() { return requiredHours; }
    public String getRewardCommand() { return rewardCommand; }
    public String getRewardMessage() { return rewardMessage; }
}