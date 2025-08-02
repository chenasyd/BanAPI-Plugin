package org.a.banapi.config;

import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ConfigManager {
    private final JavaPlugin plugin;
    private Map<String, Object> config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        try (InputStream input = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            config = yaml.load(input);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load config.yml: " + e.getMessage());
        }
    }

    public String getApiUrl() {
        return getNestedConfig("api.url", "http://localhost:5000");
    }

    public String getApiKey() {
        return getNestedConfig("api.key", "");
    }

    public int getUpdateInterval() {
        return getNestedConfig("update-interval", 60);
    }

    public boolean isBanBroadcastEnabled() {
        return getNestedConfig("broadcast.enabled", true);
    }

    public String getBanBroadcastFormat() {
        return getNestedConfig("broadcast.format", 
            "§c[封禁公告] 玩家 §e{nickname} §c已被管理员 §e{admin} §c封禁，原因: §e{reason}\n§7服务器累计封禁数: §f{total}");
    }
    
    /**
     * 获取封禁消息格式
     * @return 封禁消息格式字符串，支持多种占位符
     */
    public String getBanMessageFormat() {
        return getNestedConfig("ban-message.format", 
            "§4§l您已被服务器封禁!\n" +
            "§c▶ §f原因: §e{reason}\n" +
            "§c▶ §f管理员: §e{admin}\n" +
            "§c▶ §f封禁时间: §e{startTime}\n" +
            "§c▶ §f封禁类型: §e{banType}\n" +
            "§c▶ §f解封时间: §e{endTime}\n" +
            "§c▶ §f封禁ID: §7#{id}\n\n" +
            "§7如有异议，请联系管理员或在官网申诉");
    }
    
    /**
     * 检查是否启用公共API
     * @return 是否启用公共API
     */
    public boolean isPublicApiEnabled() {
        return getNestedConfig("public-api.enabled", true);
    }
    
    /**
     * 获取公共API警告消息格式
     * @return 警告消息格式字符串
     */
    public String getPublicApiWarningFormat() {
        return getNestedConfig("public-api.warning-format", 
            "§c[BanAPI预警] §e玩家 {nickname} §c在公共封禁列表中\n" +
            "§7IP: §f{ip}\n" +
            "§7原因: §f{reason}\n" +
            "§7封禁时间: §f{timestamp}");
    }
    
    /**
     * 检查是否记录警告日志
     * @return 是否记录警告日志
     */
    public boolean isLogWarningsEnabled() {
        return getNestedConfig("public-api.log-warnings", true);
    }
    
    /**
     * 检查是否启用BungeeCord支持
     * @return 是否启用BungeeCord支持
     */
    public boolean isBungeeEnabled() {
        return getNestedConfig("bungee.enabled", false);
    }
    
    /**
     * 获取BungeeCord通道名称
     * @return BungeeCord通道名称
     */
    public String getBungeeChannel() {
        return getNestedConfig("bungee.channel", "BungeeCord");
    }

    // 辅助方法：获取嵌套配置值
    @SuppressWarnings("unchecked")
    private <T> T getNestedConfig(String path, T defaultValue) {
        if (config == null) {
            return defaultValue;
        }

        String[] parts = path.split("\\.");
        Map<String, Object> current = config;

        for (int i = 0; i < parts.length - 1; i++) {
            Object value = current.get(parts[i]);
            if (value instanceof Map) {
                current = (Map<String, Object>) value;
            } else {
                return defaultValue;
            }
        }

        Object value = current.get(parts[parts.length - 1]);
        return value != null ? (T) value : defaultValue;
    }


}
