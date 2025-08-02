package org.a.banapi.tasks;

import org.a.banapi.Banapi;
import org.a.banapi.api.APIService;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class BanUpdateTask extends BukkitRunnable {
    private final Banapi plugin;
    private final APIService apiService;

    public BanUpdateTask(Banapi plugin, APIService apiService) {
        this.plugin = plugin;
        this.apiService = apiService;
    }

    @Override
    public void run() {
        try {
            List<Map<String, Object>> bans = apiService.getBans();
            plugin.getLogger().info("成功获取到 " + bans.size() + " 条封禁记录");

            // 每10分钟清理一次已公告列表
            if (System.currentTimeMillis() % 600000 < 50) {
                plugin.getNotifiedBans().clear();
                plugin.getLogger().info("已清理封禁公告缓存");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "获取封禁数据失败: " + e.getMessage(), e);
        }
    }

    public void start() {
        int interval = apiService.getConfigManager().getUpdateInterval();
        this.runTaskTimerAsynchronously(plugin, 0, interval * 20L);
    }
}