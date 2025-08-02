package org.a.banapi.listeners;

import org.a.banapi.Banapi;
import org.a.banapi.api.APIService;
import org.a.banapi.api.PublicAPIService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 处理玩家登录事件的监听器
 */
public class PlayerLoginListener implements Listener {
    private final Banapi plugin;
    private final APIService apiService;
    private final PublicAPIService publicAPIService;
    private final Logger logger;

    public PlayerLoginListener(Banapi plugin, APIService apiService, PublicAPIService publicAPIService) {
        this.plugin = plugin;
        this.apiService = apiService;
        this.publicAPIService = publicAPIService;
        this.logger = Logger.getLogger("BanAPI");
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        String playerName = event.getPlayer().getName();
        InetAddress address = event.getAddress();
        String ip = address.getHostAddress();

        // 检查ID是否包含禁止关键词
        if (playerName.toLowerCase().contains("api")) {
            event.disallow(Result.KICK_OTHER, "§c您的游戏ID包含禁止使用的关键词 'API'\n§7请更换其他游戏ID后再尝试登录");
            return;
        }

        try {
            // 检查本地封禁API
            List<Map<String, Object>> banList = apiService.getBans();
            Map<String, Object> banInfo = findPlayerBan(banList, playerName);

            if (banInfo != null) {
                if (!isBanValid(banInfo)) {
                    event.disallow(Result.KICK_BANNED, "§c无法验证您的封禁状态\n§7请联系管理员");
                    return;
                }

                if (!isBanReleased(banInfo)) {
                    String banMessage = buildBanMessage(banInfo);
                    event.disallow(Result.KICK_BANNED, banMessage);

                    // 广播封禁消息
                    if (plugin.getConfigManager().isBanBroadcastEnabled()) {
                        broadcastBanMessage(playerName, banInfo);
                    }
                    return;
                }
            }

            // 检查公共封禁API
            checkPublicBanAPI(event, playerName, ip);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "处理玩家登录时出错", e);
            event.allow();
        }
    }
    
    /**
     * 检查玩家是否在公共封禁API中
     * @param event 登录事件
     * @param playerName 玩家名称
     * @param ip 玩家IP
     */
    private void checkPublicBanAPI(PlayerLoginEvent event, String playerName, String ip) {
        try {
            // 异步检查，避免阻塞主线程
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    // 检查玩家名称
                    Map<String, Object> playerBanInfo = publicAPIService.checkPlayerBan(playerName);
                    if (playerBanInfo != null) {
                        // 记录警告日志
                        publicAPIService.logWarning(playerName, ip, playerBanInfo);
                        
                        // 通知在线OP
                        publicAPIService.notifyOps(playerName, playerBanInfo);
                    }
                    
                    // 检查IP地址
                    Map<String, Object> ipBanInfo = publicAPIService.checkIpBan(ip);
                    if (ipBanInfo != null && playerBanInfo == null) { // 避免重复通知
                        // 记录警告日志
                        publicAPIService.logWarning(playerName, ip, ipBanInfo);
                        
                        // 通知在线OP
                        publicAPIService.notifyOps(playerName, ipBanInfo);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "检查公共封禁API时出错", e);
                }
            });
            
            // 允许玩家登录，因为公共API只是警告不阻止
            event.allow();
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "处理公共封禁API检查时出错", e);
            event.allow(); // 出错时允许登录
        }
    }

    private Map<String, Object> findPlayerBan(List<Map<String, Object>> banList, String playerName) {
        return banList.stream()
                .filter(ban -> playerName.equalsIgnoreCase((String) ban.get("nickname")))
                .findFirst()
                .orElse(null);
    }

    private boolean isBanValid(Map<String, Object> banInfo) {
        return banInfo.containsKey("isReleased") && banInfo.containsKey("isPermanent");
    }

    private boolean isBanReleased(Map<String, Object> banInfo) {
        return Boolean.TRUE.equals(banInfo.get("isReleased"));
    }

    private String buildBanMessage(Map<String, Object> banInfo) {
        // 获取配置的封禁消息格式
        String messageFormat = plugin.getConfigManager().getBanMessageFormat();
        
        // 获取封禁信息
        int id = ((Number) banInfo.getOrDefault("id", 0)).intValue();
        String reason = (String) banInfo.getOrDefault("reason", "违反服务器规则");
        String admin = (String) banInfo.getOrDefault("admin", "系统");
        boolean permanent = Boolean.TRUE.equals(banInfo.get("isPermanent"));
        String startTime = formatTime((String) banInfo.getOrDefault("startTime", "未知时间"));
        String endTime = permanent ? "永久" : formatTime((String) banInfo.getOrDefault("endTime", "未知"));
        String banType = permanent ? "永久封禁" : "临时封禁";
        
        // 替换占位符
        return messageFormat
            .replace("{id}", String.valueOf(id))
            .replace("{reason}", reason)
            .replace("{admin}", admin)
            .replace("{startTime}", startTime)
            .replace("{endTime}", endTime)
            .replace("{banType}", banType)
            .replace("{isPermanent}", String.valueOf(permanent));
    }
    
    /**
     * 格式化时间为更易读的格式
     */
    private String formatTime(String timeStr) {
        if (timeStr == null || timeStr.equals("未知时间") || timeStr.equals("未知")) {
            return timeStr;
        }
        
        try {
            // 将ISO 8601格式转换为更易读的格式
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = isoFormat.parse(timeStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return timeStr; // 如果解析失败，返回原始字符串
        }
    }

    private void broadcastBanMessage(String playerName, Map<String, Object> banInfo) {
        // 检查是否已经公告过
        if (plugin.getNotifiedBans().contains(playerName)) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<String, Object> stats = apiService.getStats();
                String message = plugin.getConfigManager().getBanBroadcastFormat()
                        .replace("{nickname}", playerName)
                        .replace("{reason}", (String) banInfo.getOrDefault("reason", "无"))
                        .replace("{admin}", (String) banInfo.getOrDefault("admin", "系统"))
                        .replace("{total}", String.valueOf(stats.get("total")));

                // 标记为已公告
                plugin.getNotifiedBans().add(playerName);

                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.broadcastMessage(message));
            } catch (Exception e) {
                logger.log(Level.WARNING, "广播封禁消息时出错", e);
            }
        });
    }
}