package org.a.banapi.commands;

import org.a.banapi.Banapi;
import org.a.banapi.api.APIService;
import org.a.banapi.config.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * 获取API信息的命令
 */
public class GetAPICommand implements CommandExecutor {

    private final Banapi plugin;
    private final APIService apiService;

    public GetAPICommand(Banapi plugin) {
        this.plugin = plugin;
        this.apiService = plugin.getApiService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查权限
        if (!sender.hasPermission("banapi.getapi")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return true;
        }

        // 获取API信息
        ConfigManager configManager = apiService.getConfigManager();
        String apiUrl = configManager.getApiUrl();
        String apiKey = configManager.getApiKey();
        int updateInterval = configManager.getUpdateInterval();

        // 隐藏API Key的部分内容
        String maskedApiKey = maskApiKey(apiKey);

        // 发送API信息
        sender.sendMessage(ChatColor.GREEN + "===== BanAPI 信息 =====");
        sender.sendMessage(ChatColor.YELLOW + "API URL: " + ChatColor.WHITE + apiUrl);
        sender.sendMessage(ChatColor.YELLOW + "API Key: " + ChatColor.WHITE + maskedApiKey);
        sender.sendMessage(ChatColor.YELLOW + "更新间隔: " + ChatColor.WHITE + updateInterval + " 秒");
        sender.sendMessage(ChatColor.GREEN + "=====================");

        return true;
    }

    /**
     * 隐藏API Key的部分内容，只显示前4位和后4位
     * @param apiKey 完整的API Key
     * @return 隐藏部分内容的API Key
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return apiKey;
        }

        int length = apiKey.length();
        String prefix = apiKey.substring(0, 4);
        String suffix = apiKey.substring(length - 4);
        return prefix + "****" + suffix;
    }
}
