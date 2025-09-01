package voideria.voiderialpc;

import org.bstats.bukkit.Metrics;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class VoideriaYetkiliSure extends JavaPlugin {
    // Yönetici bileşenleri
    private DataManager dataManager;
    private RankManager rankManager;
    private DiscordReporter discordReporter;
    
    // Görevler
    private ScheduledTask trackingTask;
    private ScheduledTask rewardTask;
    private ScheduledTask resetTask;
    private ScheduledTask dailyReportTask;
    private ScheduledTask offlineRewardTask;
    
    // Ayarlar
    private boolean debugMode;
    private final AtomicBoolean resetInProgress = new AtomicBoolean(false);
    private final ZoneId serverTimeZone = ZoneId.of("Europe/Istanbul");

    @Override
    public void onEnable() {
        try {
            // Temel kurulum
            initializeConfig();
            initializeManagers();
            registerListenersAndCommands();
            
            // Metrik ve istatistikler
            setupMetrics();
            
            // Periyodik görevler
            startAllTasks();
            
            getLogger().info("Plugin başarıyla etkinleştirildi!");
        } catch (Exception e) {
            handleStartupError(e);
        }
    }

    @Override
    public void onDisable() {
        shutdownTasks();
        saveData();
    }

    // Başlangıç metodları
    private void initializeConfig() {
        saveDefaultConfig();
        reloadConfig();
        debugMode = getConfig().getBoolean("debug-mode", false);
    }

    private void initializeManagers() {
        dataManager = new DataManager(this);
        rankManager = new RankManager(this, dataManager);
        discordReporter = new DiscordReporter(this, dataManager);
    }

    private void registerListenersAndCommands() {
        getServer().getPluginManager().registerEvents(new PlayTimeTracker(this, dataManager), this);
        getCommand("yetkilisure").setExecutor(new YetkiliSureCommand(this, dataManager, rankManager, discordReporter));
    }

    private void setupMetrics() {
        int pluginId = 26753;
        new Metrics(this, pluginId);
    }

    // Görev yönetimi
    private void startAllTasks() {
        startTrackingTask();
        startRewardTask();
        startResetTask();
        startDailyReportTask();
        startOfflineRewardTask();
    }

    private void shutdownTasks() {
        cancelTask(trackingTask);
        cancelTask(rewardTask);
        cancelTask(resetTask);
        cancelTask(dailyReportTask);
        cancelTask(offlineRewardTask);
    }

    // Özel görevler
    private void startTrackingTask() {
        trackingTask = getServer().getGlobalRegionScheduler().runAtFixedRate(
            this,
            task -> trackOnlinePlayers(),
            20, 20
        );
    }

    private void trackOnlinePlayers() {
        if (debugMode) getLogger().info("Oyuncu süre takibi başladı");
        
        for (Player player : getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (dataManager.isPlayerTracked(uuid)) {
                dataManager.updatePlayerTime(uuid, player.getName());
                
                if (debugMode && player.getTicksLived() % 200 == 0) {
                    logPlayerTime(player);
                }
            }
        }
    }

    private void logPlayerTime(Player player) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data != null) {
            getLogger().info(player.getName() + " - " + data.getTotalSeconds() + " saniye");
        }
    }

    private void startRewardTask() {
        rewardTask = getServer().getGlobalRegionScheduler().runAtFixedRate(
            this,
            task -> checkOnlinePlayersRewards(),
            1200, 6000
        );
    }

    private void checkOnlinePlayersRewards() {
        if (debugMode) getLogger().info("Online oyuncular için ödül kontrolü");
        
        for (Player player : getServer().getOnlinePlayers()) {
            if (dataManager.isPlayerTracked(player.getUniqueId())) {
                rankManager.checkPlayerRewards(player);
            }
        }
    }

    private void startOfflineRewardTask() {
        if (!getConfig().getBoolean("offline-rewards.enabled", true)) {
            getLogger().info("Offline ödül kontrolü kapalı");
            return;
        }

        int interval = getConfig().getInt("offline-rewards.check-interval", 60) * 60 * 20;
        offlineRewardTask = getServer().getGlobalRegionScheduler().runAtFixedRate(
            this,
            task -> checkOfflinePlayersRewards(),
            interval, interval
        );
    }

    private void checkOfflinePlayersRewards() {
        if (debugMode) getLogger().info("Offline oyuncular için ödül kontrolü");
        
        int rewarded = 0;
        for (PlayerData data : dataManager.getAllData().values()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(data.getUuid());
            if (!player.isOnline()) {
                rankManager.checkPlayerRewards(player);
                rewarded++;
            }
        }
        
        if (debugMode && rewarded > 0) {
            getLogger().info(rewarded + " offline oyuncu kontrol edildi");
        }
    }

    private void startResetTask() {
        if (!getConfig().getBoolean("reset.enabled", true)) {
            getLogger().info("Sıfırlama sistemi kapalı");
            return;
        }

        resetTask = getServer().getGlobalRegionScheduler().runAtFixedRate(
            this,
            task -> checkResetTime(),
            600, 600
        );
    }

    private void checkResetTime() {
        try {
            ZonedDateTime now = ZonedDateTime.now(serverTimeZone);
            DayOfWeek resetDay = DayOfWeek.valueOf(getConfig().getString("reset.day-of-week", "SUNDAY").toUpperCase());
            LocalTime resetTime = LocalTime.parse(getConfig().getString("reset.time", "23:59"), DateTimeFormatter.ofPattern("HH:mm"));
            int delayMinutes = getConfig().getInt("reset.reset-delay-minutes", 1);

            if (now.getDayOfWeek() == resetDay && now.getHour() == resetTime.getHour() && now.getMinute() == resetTime.getMinute()) {
                executeResetWithDelay(delayMinutes);
            }
        } catch (Exception e) {
            getLogger().warning("Sıfırlama kontrolü hatası: " + e.getMessage());
        }
    }

    private void executeResetWithDelay(int delayMinutes) {
        if (resetInProgress.compareAndSet(false, true)) {
            getServer().getGlobalRegionScheduler().runDelayed(
                this,
                task -> {
                    checkAllPlayersRewards();
                    resetAllTimes();
                    resetInProgress.set(false);
                },
                TimeUnit.MINUTES.toSeconds(delayMinutes) * 20
            );
        }
    }

    private void startDailyReportTask() {
        if (!getConfig().getBoolean("daily-report.enabled", false)) {
            getLogger().info("Günlük rapor kapalı");
            return;
        }

        dailyReportTask = getServer().getGlobalRegionScheduler().runAtFixedRate(
            this,
            task -> sendDailyReport(),
            1200, 1200
        );
    }

    private void sendDailyReport() {
        try {
            LocalTime reportTime = LocalTime.parse(getConfig().getString("daily-report.time", "18:00"), DateTimeFormatter.ofPattern("HH:mm"));
            ZonedDateTime now = ZonedDateTime.now(serverTimeZone);

            if (now.getHour() == reportTime.getHour() && now.getMinute() == reportTime.getMinute()) {
                if (getConfig().getBoolean("discord.enabled", false)) {
                    discordReporter.sendReport().thenAccept(success -> {
                        if (success) {
                            getLogger().info("Günlük Discord raporu gönderildi");
                        } else {
                            getLogger().warning("Günlük Discord raporu gönderilemedi");
                        }
                    });
                }
            }
        } catch (Exception e) {
            getLogger().warning("Günlük rapor hatası: " + e.getMessage());
        }
    }

    // Yardımcı metodlar
    public void checkAllPlayersRewards() {
        getServer().getGlobalRegionScheduler().run(this, task -> {
            // Online oyuncular
            for (Player player : getServer().getOnlinePlayers()) {
                rankManager.checkPlayerRewards(player);
            }
            
            // Offline oyuncular
            for (PlayerData data : dataManager.getAllData().values()) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(data.getUuid());
                if (!player.isOnline()) {
                    rankManager.checkPlayerRewards(player);
                }
            }
        });
    }

    public void resetAllTimes() {
        for (PlayerData data : dataManager.getAllData().values()) {
            data.resetTime();
        }
        dataManager.saveAllData();
        getLogger().info("Tüm süreler sıfırlandı");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        debugMode = getConfig().getBoolean("debug-mode", false);
        rankManager.loadRanks();
        
        // Görevleri yeniden başlat
        shutdownTasks();
        startAllTasks();
    }

    private void saveData() {
        try {
            dataManager.saveAllData();
            getLogger().info("Veriler başarıyla kaydedildi");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Veri kaydetme hatası", e);
        }
    }

    private void cancelTask(ScheduledTask task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    private void handleStartupError(Exception e) {
        getLogger().log(Level.SEVERE, "Başlatma sırasında kritik hata", e);
        getServer().getPluginManager().disablePlugin(this);
    }
}