package voideria.voiderialpc;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.UUID;
import java.util.logging.Level;

public class PlayTimeTracker implements Listener {
    private final JavaPlugin plugin;
    private final DataManager dataManager;

    public PlayTimeTracker(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        try {
            Player player = e.getPlayer();
            UUID uuid = player.getUniqueId();
            
            if (dataManager.isPlayerTracked(uuid)) {
                dataManager.getOrCreatePlayerData(uuid, player.getName());
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "PlayerJoinEvent hatasi", ex);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e) {
        try {
            Player player = e.getPlayer();
            UUID uuid = player.getUniqueId();
            
            if (dataManager.isPlayerTracked(uuid)) {
                dataManager.savePlayerData(uuid);
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "PlayerQuitEvent hatasi", ex);
        }
    }
}