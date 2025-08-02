package org.a.banapi.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.a.banapi.Banapi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

/**
 * 处理公共封禁API的服务类
 */
public class PublicAPIService {
    private static final String API_URL = "https://api.ndp.codewaves.cn/bans";
    private final Banapi plugin;
    private final Logger logger;
    private final Logger warningLogger;
    private Map<String, Object> cachedData;
    private long lastFetchTime = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5分钟缓存
    private final org.a.banapi.config.ConfigManager configManager;

    public PublicAPIService(Banapi plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.logger = plugin.getLogger();
        this.warningLogger = Logger.getLogger("BanAPI-Warnings");

        // 设置警告日志
        if (configManager.isLogWarningsEnabled()) {
            try {
                FileHandler fileHandler = new FileHandler(plugin.getDataFolder() + "/warnings.log", true);
                fileHandler.setFormatter(new SimpleFormatter());
                warningLogger.addHandler(fileHandler);
                warningLogger.setLevel(Level.INFO);
            } catch (IOException e) {
                logger.severe("无法创建警告日志文件: " + e.getMessage());
            }
        }
    }

    /**
     * 获取公共API的封禁数据
     * @return 封禁数据
     * @throws IOException 如果API请求失败
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getBanData() throws IOException {
        long currentTime = System.currentTimeMillis();

        // 如果缓存有效，直接返回缓存数据
        if (cachedData != null && (currentTime - lastFetchTime) < CACHE_DURATION) {
            return cachedData;
        }

        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("API请求失败，响应码: " + responseCode);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = reader.lines().collect(Collectors.joining());
            Gson gson = new Gson();
            cachedData = gson.fromJson(response, Map.class);
            lastFetchTime = currentTime;
            return cachedData;
        }
    }

    /**
     * 检查玩家是否在公共封禁列表中
     * @param playerName 玩家名称
     * @return 如果玩家在封禁列表中，返回封禁信息；否则返回null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkPlayerBan(String playerName) {
        try {
            Map<String, Object> banData = getBanData();
            List<Map<String, Object>> activePlayers = (List<Map<String, Object>>) banData.get("active_players");

            if (activePlayers == null) {
                return null;
            }

            return activePlayers.stream()
                    .filter(player -> playerName.equalsIgnoreCase((String) player.get("username")))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            logger.warning("检查玩家 " + playerName + " 的公共封禁状态时出错: " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查IP是否在公共封禁列表中
     * @param ip IP地址
     * @return 如果IP在封禁列表中，返回封禁信息；否则返回null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkIpBan(String ip) {
        try {
            Map<String, Object> banData = getBanData();
            List<Map<String, Object>> activeIps = (List<Map<String, Object>>) banData.get("active_ips");

            if (activeIps == null) {
                return null;
            }

            return activeIps.stream()
                    .filter(ipData -> ip.equals(ipData.get("ip")))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            logger.warning("检查IP " + ip + " 的公共封禁状态时出错: " + e.getMessage());
            return null;
        }
    }

    /**
     * 记录警告日志
     * @param playerName 玩家名称
     * @param ip 玩家IP
     * @param banInfo 封禁信息
     */
    public void logWarning(String playerName, String ip, Map<String, Object> banInfo) {
        if (!configManager.isLogWarningsEnabled() || !configManager.isPublicApiEnabled()) {
            return;
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = dateFormat.format(new Date());

        String cause = banInfo.containsKey("cause") ? (String) banInfo.get("cause") : "未知原因";
        String banTimestamp = banInfo.containsKey("timestamp") ? (String) banInfo.get("timestamp") : "未知时间";

        String logMessage = String.format("[%s] 预警: 玩家 %s (IP: %s) 尝试登录，但在公共封禁列表中。原因: %s, 封禁时间: %s",
                timestamp, playerName, ip, cause, banTimestamp);

        warningLogger.info(logMessage);
    }

    /**
     * 向所有在线OP发送警告消息
     * @param playerName 玩家名称
     * @param banInfo 封禁信息
     */
    public void notifyOps(String playerName, Map<String, Object> banInfo) {
        if (!configManager.isPublicApiEnabled()) {
            return;
        }
        
        String cause = banInfo.containsKey("cause") ? (String) banInfo.get("cause") : "未知原因";
        String ip = banInfo.containsKey("ip") ? (String) banInfo.get("ip") : "未知IP";
        String banTimestamp = banInfo.containsKey("timestamp") ? (String) banInfo.get("timestamp") : "未知时间";

        String warningMessage = configManager.getPublicApiWarningFormat()
            .replace("{nickname}", playerName)
            .replace("{ip}", ip)
            .replace("{reason}", cause)
            .replace("{timestamp}", formatTime(banTimestamp));

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOp() || player.hasPermission("banapi.alerts")) {
                    player.sendMessage(warningMessage);
                }
            }
        });
    }

    /**
     * 格式化时间为更易读的格式
     */
    private String formatTime(String timeStr) {
        if (timeStr == null || timeStr.equals("未知时间") || timeStr.equals("未知")) {
            return timeStr;
        }

        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = isoFormat.parse(timeStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return timeStr;
        }
    }

    /**
     * 获取公共API的统计信息
     * @return 统计信息
     */
    public Map<String, Object> getStats() {
        try {
            Map<String, Object> banData = getBanData();
            Map<String, Object> stats = new HashMap<>();

            stats.put("player_count", banData.getOrDefault("player_count", 0));
            stats.put("ip_count", banData.getOrDefault("ip_count", 0));

            return stats;
        } catch (Exception e) {
            logger.warning("获取公共API统计信息时出错: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}
