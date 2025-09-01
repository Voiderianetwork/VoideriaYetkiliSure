package voideria.voiderialpc;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class DataManager {
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final ReentrantLock saveLock = new ReentrantLock();
    private File dataFile;
    private final AsyncScheduler asyncScheduler = Bukkit.getAsyncScheduler();

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        try {
            plugin.getDataFolder().mkdirs();
            dataFile = new File(plugin.getDataFolder(), "data.yml");
            
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }
            
            loadAllData();
        } catch (IOException e) {
            plugin.getLogger().severe("DataManager baslatilamadi: " + e.getMessage());
        }
    }

    private void loadAllData() {
        playerDataMap.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long totalSeconds = config.getLong(key + ".totalSeconds");
                String name = config.getString(key + ".name", "Unknown");
                PlayerData data = new PlayerData(uuid, name, totalSeconds);
                
                if (config.contains(key + ".rewards")) {
                    for (String reward : config.getStringList(key + ".rewards")) {
                        data.addReward(reward);
                    }
                }
                
                playerDataMap.put(uuid, data);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Gecersiz UUID: " + key);
            }
        }
    }

    public void saveAllData() {
        asyncScheduler.runNow(plugin, task -> {
            saveLock.lock();
            try {
                File tempFile = new File(plugin.getDataFolder(), "data.tmp");
                YamlConfiguration tempConfig = new YamlConfiguration();
                
                for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
                    String key = entry.getKey().toString();
                    PlayerData data = entry.getValue();
                    tempConfig.set(key + ".name", data.getPlayerName());
                    tempConfig.set(key + ".totalSeconds", data.getTotalSeconds());
                    tempConfig.set(key + ".rewards", new ArrayList<>(data.getRewards()));
                }
                
                tempConfig.save(tempFile);
                
                if (dataFile.delete() || !dataFile.exists()) {
                    if (!tempFile.renameTo(dataFile)) {
                        plugin.getLogger().warning("Dosya ismi degistirilemedi!");
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Veri kaydedilemedi: " + e.getMessage());
            } finally {
                saveLock.unlock();
            }
        });
    }

    public void savePlayerData(UUID uuid) {
        asyncScheduler.runNow(plugin, task -> {
            saveLock.lock();
            try {
                PlayerData data = playerDataMap.get(uuid);
                if (data != null) {
                    File tempFile = new File(plugin.getDataFolder(), "data.tmp");
                    YamlConfiguration tempConfig = new YamlConfiguration();
                    
                    // Tüm verileri yeniden yaz
                    for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
                        String key = entry.getKey().toString();
                        PlayerData d = entry.getValue();
                        tempConfig.set(key + ".name", d.getPlayerName());
                        tempConfig.set(key + ".totalSeconds", d.getTotalSeconds());
                        tempConfig.set(key + ".rewards", new ArrayList<>(d.getRewards()));
                    }
                    
                    tempConfig.save(tempFile);
                    
                    if (dataFile.delete() || !dataFile.exists()) {
                        if (!tempFile.renameTo(dataFile)) {
                            plugin.getLogger().warning("Dosya ismi degistirilemedi!");
                        }
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Oyuncu verisi kaydedilemedi: " + e.getMessage());
            } finally {
                saveLock.unlock();
            }
        });
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public PlayerData getOrCreatePlayerData(UUID uuid, String name) {
        return playerDataMap.computeIfAbsent(uuid, k -> {
            PlayerData newData = new PlayerData(uuid, name, 0);
            savePlayerData(uuid);
            return newData;
        });
    }

    public void updatePlayerTime(UUID uuid, String name) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            data.addSeconds(1);
        }
    }

    public Map<UUID, PlayerData> getAllData() {
        return new HashMap<>(playerDataMap);
    }

    public void removePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.remove(uuid);
        if (data != null) {
            asyncScheduler.runNow(plugin, task -> {
                saveLock.lock();
                try {
                    // Tüm verileri yeniden yaz
                    File tempFile = new File(plugin.getDataFolder(), "data.tmp");
                    YamlConfiguration tempConfig = new YamlConfiguration();
                    
                    for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
                        String key = entry.getKey().toString();
                        PlayerData d = entry.getValue();
                        tempConfig.set(key + ".name", d.getPlayerName());
                        tempConfig.set(key + ".totalSeconds", d.getTotalSeconds());
                        tempConfig.set(key + ".rewards", new ArrayList<>(d.getRewards()));
                    }
                    
                    tempConfig.save(tempFile);
                    
                    if (dataFile.delete() || !dataFile.exists()) {
                        if (!tempFile.renameTo(dataFile)) {
                            plugin.getLogger().warning("Dosya ismi degistirilemedi!");
                        }
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("Silme islemi basarisiz: " + e.getMessage());
                } finally {
                    saveLock.unlock();
                }
            });
        }
    }
    
    public boolean isPlayerTracked(UUID uuid) {
        return playerDataMap.containsKey(uuid);
    }
    
    public UUID getPlayerUUID(String playerName) {
        for (PlayerData data : playerDataMap.values()) {
            if (data.getPlayerName().equalsIgnoreCase(playerName)) {
                return data.getUuid();
            }
        }
        return null;
    }
    
    public Set<String> getAllPlayerNames() {
        Set<String> names = new HashSet<>();
        for (PlayerData data : playerDataMap.values()) {
            names.add(data.getPlayerName());
        }
        return names;
    }
}