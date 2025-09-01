package voideria.voiderialpc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.*;

public class YetkiliSureCommand implements CommandExecutor, TabCompleter {
    private final VoideriaYetkiliSure plugin;
    private final DataManager dataManager;
    private final RankManager rankManager;
    private final DiscordReporter discordReporter;

    public YetkiliSureCommand(VoideriaYetkiliSure plugin, DataManager dataManager, 
                             RankManager rankManager, DiscordReporter discordReporter) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.rankManager = rankManager;
        this.discordReporter = discordReporter;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) return showHelp(sender);

        switch (args[0].toLowerCase()) {
            case "ver": return handleGrant(sender, args);
            case "sil": return handleRemove(sender, args);
            case "odulver": return handleReward(sender, args);
            case "liste": return handleList(sender);
            case "ekle": return handleAddTime(sender, args);
            case "cikar": return handleRemoveTime(sender, args);
            case "yenile": return handleReload(sender);
            case "rapor": return handleReport(sender);
            case "sifirla": return handleReset(sender);
            case "gunlukrapor": return handleDailyReport(sender, args);
            case "yardim": return showHelp(sender);
            default: return showHelp(sender);
        }
    }

    private boolean handleGrant(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /yetkilisure ver <oyuncu> <rütbe>");
            return true;
        }
        
        String playerName = args[1];
        if (!isValidPlayerName(playerName)) {
            sender.sendMessage(ChatColor.RED + "Geçersiz oyuncu adı! Sadece harf, sayı ve alt çizgi kullanabilirsiniz (3-16 karakter)");
            return true;
        }
        
        // Online oyuncu kontrolü
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            rankManager.grantRank(onlinePlayer, args[2]);
            sender.sendMessage(ChatColor.GREEN + onlinePlayer.getName() + " oyuncusuna " + args[2] + " rütbesi verildi!");
            return true;
        }
        
        // Offline oyuncu kontrolü
        UUID playerId = findOrCreatePlayer(playerName);
        if (playerId == null) {
            sender.sendMessage(ChatColor.RED + "Oyuncu bulunamadı veya hiç oyuna giriş yapmamış!");
            return true;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        rankManager.grantRank(offlinePlayer, args[2]);
        sender.sendMessage(ChatColor.GREEN + offlinePlayer.getName() + " oyuncusuna " + args[2] + " rütbesi verildi (offline)!");
        return true;
    }
    
    private boolean isValidPlayerName(String name) {
        return name != null && name.length() >= 3 && name.length() <= 16 && 
               name.matches("^[a-zA-Z0-9_]+$");
    }

    private UUID findOrCreatePlayer(String playerName) {
        // Önce veritabanında ara
        UUID uuid = dataManager.getPlayerUUID(playerName);
        if (uuid != null) return uuid;

        // Bukkit'in offline oyuncu listesinde ara
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null && 
                offlinePlayer.getName().equalsIgnoreCase(playerName)) {
                // Yeni kayıt oluştur
                dataManager.getOrCreatePlayerData(offlinePlayer.getUniqueId(), playerName);
                return offlinePlayer.getUniqueId();
            }
        }
        return null;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /yetkilisure sil <oyuncu>");
            return true;
        }
        
        String playerName = args[1];
        if (!isValidPlayerName(playerName)) {
            sender.sendMessage(ChatColor.RED + "Geçersiz oyuncu adı!");
            return true;
        }
        
        // Online oyuncu kontrolü
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            rankManager.revokeAllRanks(onlinePlayer);
            sender.sendMessage(ChatColor.GREEN + onlinePlayer.getName() + " oyuncusunun tüm verileri silindi!");
            return true;
        }
        
        // Offline oyuncu kontrolü
        UUID playerId = dataManager.getPlayerUUID(playerName);
        if (playerId == null) {
            sender.sendMessage(ChatColor.RED + "Oyuncu bulunamadı veya hiç verisi yok!");
            return true;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        rankManager.revokeAllRanks(offlinePlayer);
        sender.sendMessage(ChatColor.GREEN + offlinePlayer.getName() + " oyuncusunun tüm verileri silindi (offline)!");
        return true;
    }

    private boolean handleAddTime(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /yetkilisure ekle <oyuncu> <dakika>");
            return true;
        }

        try {
            int minutes = Integer.parseInt(args[2]);
            if (minutes <= 0) {
                sender.sendMessage(ChatColor.RED + "Geçersiz dakika değeri! 0'dan büyük bir sayı girin.");
                return true;
            }
            
            String playerName = args[1];
            if (!isValidPlayerName(playerName)) {
                sender.sendMessage(ChatColor.RED + "Geçersiz oyuncu adı!");
                return true;
            }
            
            // Online oyuncu kontrolü
            Player onlinePlayer = Bukkit.getPlayer(playerName);
            if (onlinePlayer != null) {
                rankManager.addTime(onlinePlayer.getUniqueId(), minutes, onlinePlayer.getName());
                sender.sendMessage(ChatColor.GREEN + onlinePlayer.getName() + " oyuncusuna " + minutes + " dakika eklendi!");
                return true;
            }
            
            // Offline oyuncu kontrolü
            UUID playerId = findOrCreatePlayer(playerName);
            if (playerId == null) {
                sender.sendMessage(ChatColor.RED + "Oyuncu bulunamadı!");
                return true;
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
            rankManager.addTime(playerId, minutes, offlinePlayer.getName());
            sender.sendMessage(ChatColor.GREEN + offlinePlayer.getName() + " oyuncusuna " + minutes + " dakika eklendi (offline)!");
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Geçersiz dakika formatı! Sayı girmelisiniz.");
            return true;
        }
    }

    private boolean handleRemoveTime(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /yetkilisure cikar <oyuncu> <dakika>");
            return true;
        }

        try {
            int minutes = Integer.parseInt(args[2]);
            if (minutes <= 0) {
                sender.sendMessage(ChatColor.RED + "Geçersiz dakika değeri! 0'dan büyük bir sayı girin.");
                return true;
            }
            
            String playerName = args[1];
            if (!isValidPlayerName(playerName)) {
                sender.sendMessage(ChatColor.RED + "Geçersiz oyuncu adı!");
                return true;
            }
            
            // Online oyuncu kontrolü
            Player onlinePlayer = Bukkit.getPlayer(playerName);
            if (onlinePlayer != null) {
                rankManager.removeTime(onlinePlayer.getUniqueId(), minutes, onlinePlayer.getName());
                sender.sendMessage(ChatColor.GREEN + onlinePlayer.getName() + " oyuncusundan " + minutes + " dakika silindi!");
                return true;
            }
            
            // Offline oyuncu kontrolü
            UUID playerId = dataManager.getPlayerUUID(playerName);
            if (playerId == null) {
                sender.sendMessage(ChatColor.RED + "Oyuncu bulunamadı veya hiç verisi yok!");
                return true;
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
            rankManager.removeTime(playerId, minutes, offlinePlayer.getName());
            sender.sendMessage(ChatColor.GREEN + offlinePlayer.getName() + " oyuncusundan " + minutes + " dakika silindi (offline)!");
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Geçersiz dakika formatı! Sayı girmelisiniz.");
            return true;
        }
    }

    private boolean handleReward(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /yetkilisure odulver <oyuncu>");
            return true;
        }

        String playerName = args[1];
        if (!isValidPlayerName(playerName)) {
            sender.sendMessage(ChatColor.RED + "Geçersiz oyuncu adı!");
            return true;
        }
        
        // Online oyuncu kontrolü
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            rankManager.checkPlayerRewards(onlinePlayer);
            sender.sendMessage(ChatColor.GREEN + onlinePlayer.getName() + " oyuncusu ödüllendirildi!");
            return true;
        }
        
        // Offline oyuncu kontrolü
        UUID playerId = dataManager.getPlayerUUID(playerName);
        if (playerId == null) {
            sender.sendMessage(ChatColor.RED + "Oyuncu bulunamadı veya hiç oyuna giriş yapmamış!");
            return true;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        rankManager.checkPlayerRewards(offlinePlayer);
        sender.sendMessage(ChatColor.GREEN + offlinePlayer.getName() + " oyuncusu ödüllendirildi (offline)!");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Map<UUID, PlayerData> allData = dataManager.getAllData();
        if (allData.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Kayıtlı oyuncu bulunamadı.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "===== Yetkili Süre Listesi =====");
        allData.values().stream()
            .sorted((d1, d2) -> Long.compare(d2.getTotalSeconds(), d1.getTotalSeconds()))
            .forEach(data -> {
                String playerName = data.getPlayerName();
                String time = data.getFormattedTime();
                String status = Bukkit.getOfflinePlayer(data.getUuid()).isOnline() ? 
                    ChatColor.GREEN + "Çevrimiçi" : ChatColor.GRAY + "Çevrimdışı";
                sender.sendMessage(ChatColor.AQUA + playerName + ": " + ChatColor.WHITE + time + " - " + status);
            });
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("voideria.yetkilisure.reload")) {
            sender.sendMessage(ChatColor.RED + "Bu komutu kullanma yetkiniz yok!");
            return true;
        }
        
        plugin.reloadPluginConfig();
        sender.sendMessage(ChatColor.GREEN + "Plugin ve konfigürasyon yeniden yüklendi.");
        return true;
    }

    private boolean handleReport(CommandSender sender) {
        if (!sender.hasPermission("voideria.yetkilisure.report")) {
            sender.sendMessage(ChatColor.RED + "Bu komutu kullanma yetkiniz yok!");
            return true;
        }
        
        discordReporter.sendReport().thenAccept(success -> {
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Discord raporu gönderildi!");
            } else {
                sender.sendMessage(ChatColor.RED + "Discord raporu gönderilemedi!");
            }
        });
        return true;
    }

    private boolean handleReset(CommandSender sender) {
        if (!sender.hasPermission("voideria.yetkilisure.admin")) {
            sender.sendMessage(ChatColor.RED + "Bu komutu kullanma yetkiniz yok!");
            return true;
        }
        
        plugin.resetAllTimes();
        sender.sendMessage(ChatColor.GREEN + "Tüm yetkili süreleri sıfırlandı!");
        return true;
    }
    
    private boolean handleDailyReport(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /yetkilisure gunlukrapor <aç/kapat>");
            return true;
        }
        
        boolean enable = args[1].equalsIgnoreCase("aç");
        plugin.getConfig().set("daily-report.enabled", enable);
        plugin.saveConfig();
        plugin.reloadPluginConfig();
        
        sender.sendMessage(ChatColor.GREEN + "Günlük rapor: " + (enable ? "Açıldı" : "Kapatıldı"));
        return true;
    }

    private boolean showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== Yetkili Süre Sistemi =====");
        sender.sendMessage(ChatColor.YELLOW + "/yetkilisure ver <oyuncu> <rütbe> " + ChatColor.WHITE + "- Rütbe ver");
        sender.sendMessage(ChatColor.YELLOW + "/yetkilisure sil <oyuncu> " + ChatColor.WHITE + "- Tüm verileri sil");
        sender.sendMessage(ChatColor.YELLOW + "/yetkilisure ekle <oyuncu> <dakika> " + ChatColor.WHITE + "- Süre ekle");
        sender.sendMessage(ChatColor.YELLOW + "/yetkilisure cikar <oyuncu> <dakika> " + ChatColor.WHITE + "- Süre çıkar");
        sender.sendMessage(ChatColor.YELLOW + "/yetkilisure odulver <oyuncu> " + ChatColor.WHITE + "- Ödül ver");
        sender.sendMessage(ChatColor.YELLOW + "/yetkilisure liste " + ChatColor.WHITE + "- Liste göster");
        sender.sendMessage(ChatColor.YELLOW + "/yetkilisure rapor " + ChatColor.WHITE + "- Discord rapor gönder");
        sender.sendMessage(ChatColor.YELLOW + "/yetkilisure sifirla " + ChatColor.WHITE + "- Tüm süreleri sıfırla");
        sender.sendMessage(ChatColor.YELLOW + "/yetkilisure yenile " + ChatColor.WHITE + "- Plugin'i yenile");
        sender.sendMessage(ChatColor.YELLOW + "/yetkilisure gunlukrapor <aç/kapat> " + ChatColor.WHITE + "- Günlük rapor ayarı");
        sender.sendMessage(ChatColor.YELLOW + "/yetkilisure yardim " + ChatColor.WHITE + "- Yardım menüsü");
        
        sender.sendMessage(ChatColor.GOLD + "\nMevcut Rütbeler:");
        rankManager.getRanks().forEach((rank, config) -> {
            sender.sendMessage(ChatColor.AQUA + "- " + rank + ChatColor.WHITE + 
                             " (" + config.getRequiredHours() + " saat)");
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> commands = Arrays.asList("ver", "sil", "ekle", "cikar", "odulver", "liste", "rapor", "sifirla", "yenile", "gunlukrapor", "yardim");
            for (String cmd : commands) {
                if (cmd.startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        }
        else if (args.length == 2) {
            // Online oyuncular
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
            // Offline oyuncular
            for (String name : dataManager.getAllPlayerNames()) {
                if (name.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(name);
                }
            }
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("ver")) {
                for (String rank : rankManager.getRanks().keySet()) {
                    if (rank.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(rank);
                    }
                }
            }
            else if (args[0].equalsIgnoreCase("gunlukrapor")) {
                if ("aç".startsWith(args[2].toLowerCase())) completions.add("aç");
                if ("kapat".startsWith(args[2].toLowerCase())) completions.add("kapat");
            }
            else if (args[0].equalsIgnoreCase("ekle") || args[0].equalsIgnoreCase("cikar")) {
                completions.add("60");
                completions.add("120");
                completions.add("240");
                completions.add("480");
            }
        }
        
        return completions;
    }
}