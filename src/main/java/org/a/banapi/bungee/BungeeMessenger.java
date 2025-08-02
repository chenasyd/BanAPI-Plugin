package org.a.banapi.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.a.banapi.Banapi;
import org.bukkit.entity.Player;

/**
 * 处理与BungeeCord的通信
 */
public class BungeeMessenger {
    private final Banapi plugin;
    private final String channel;

    public BungeeMessenger(Banapi plugin, String channel) {
        this.plugin = plugin;
        this.channel = channel;

        // 注册BungeeCord通道
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
    }

    /**
     * 通过BungeeCord踢出玩家
     * @param playerName 玩家名称
     * @param reason 踢出原因
     */
    public void kickPlayer(String playerName, String reason) {
        // 由于我们可能在玩家尝试登录时就需要踢出，此时可能没有Player对象
        // 所以我们需要找一个在线玩家来发送消息
        if (plugin.getServer().getOnlinePlayers().isEmpty()) {
            plugin.getLogger().warning("无法通过BungeeCord踢出玩家 " + playerName + "：没有在线玩家可用于发送消息");
            return;
        }

        Player sender = plugin.getServer().getOnlinePlayers().iterator().next();

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("KickPlayer");
        out.writeUTF(playerName);
        out.writeUTF(reason);

        sender.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        plugin.getLogger().info("已通过BungeeCord踢出玩家 " + playerName);
    }
}
