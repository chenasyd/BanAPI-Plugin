package org.a.banapi;

import org.a.banapi.api.APIService;
import org.a.banapi.commands.BanAPICommand;
import org.a.banapi.commands.GetAPICommand;
import org.a.banapi.config.ConfigManager;
import org.a.banapi.listeners.PlayerLoginListener;
import org.a.banapi.tasks.BanUpdateTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Banapi extends JavaPlugin {
    private final Set<String> notifiedBans = Collections.synchronizedSet(new HashSet<>());
    private ConfigManager configManager;
    private APIService apiService;
    private BanUpdateTask banUpdateTask;

    @Override
    public void onEnable() {
        // 初始化配置
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        // 初始化API服务
        apiService = new APIService(configManager);

        // 启动定时任务
        banUpdateTask = new BanUpdateTask(this, apiService);
        banUpdateTask.start();

        // 注册命令
        this.getCommand("getapi").setExecutor(new GetAPICommand(this));
        this.getCommand("banapi").setExecutor(new BanAPICommand(this));

        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerLoginListener(this, apiService), this);

        getLogger().info("BanAPI插件已启用");
    }

    @Override
    public void onDisable() {
        if (banUpdateTask != null) {
            banUpdateTask.cancel();
        }

        getLogger().info("BanAPI插件已禁用");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public APIService getApiService() {
        return apiService;
    }

public Set<String> getNotifiedBans() {
    return notifiedBans;
}
}