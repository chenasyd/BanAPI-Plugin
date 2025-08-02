package org.a.banapi;

import org.a.banapi.api.APIService;
import org.a.banapi.api.PublicAPIService;
import org.a.banapi.commands.BanAPICommand;
import org.a.banapi.commands.GetAPICommand;
import org.a.banapi.commands.PublicBanCommand;
import org.a.banapi.config.ConfigManager;
import org.a.banapi.listeners.PlayerLoginListener;
import org.a.banapi.tasks.BanUpdateTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * BanAPI插件主类
 */
public final class Banapi extends JavaPlugin {
    private final Set<String> notifiedBans = Collections.synchronizedSet(new HashSet<>());
    private ConfigManager configManager;
    private APIService apiService;
    private PublicAPIService publicAPIService;
    private BanUpdateTask banUpdateTask;

    @Override
    public void onEnable() {
        // 创建插件数据文件夹
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // 初始化配置
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        // 初始化API服务
        apiService = new APIService(configManager);
        publicAPIService = new PublicAPIService(this);

        // 启动定时任务
        banUpdateTask = new BanUpdateTask(this, apiService);
        banUpdateTask.start();

        // 注册命令
        this.getCommand("getapi").setExecutor(new GetAPICommand(this));
        this.getCommand("banapi").setExecutor(new BanAPICommand(this));
        this.getCommand("publicban").setExecutor(new PublicBanCommand(this));

        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerLoginListener(this, apiService, publicAPIService), this);

        getLogger().info("BanAPI插件已启用");
        getLogger().info("已集成公共封禁API");
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

    /**
     * 获取已公告封禁玩家集合
     * @return 线程安全的已公告封禁玩家集合
     */
    public Set<String> getNotifiedBans() {
        return notifiedBans;
    }
    
    /**
     * 获取公共API服务
     * @return 公共API服务实例
     */
    public PublicAPIService getPublicAPIService() {
        return publicAPIService;
    }
}