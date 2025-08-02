package org.a.banapi.commands;

import org.a.banapi.Banapi;
import org.a.banapi.api.PublicAPIService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 处理公共封禁API的命令
 */
public class PublicBanCommand implements CommandExecutor, TabCompleter {
    private final Banapi plugin;
    private final PublicAPIService publicAPIService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public PublicBanCommand(Banapi plugin) {
        this.plugin = plugin;
        this.publicAPIService = plugin.getPublicAPIService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("banapi.publicban")) {
            sender.sendMessage(Component.text("你没有权限使用此命令！").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                showBanList(sender);
                break;
            case "stats":
                showStats(sender);
                break;
            case "check":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("用法: /publicban check <玩家名>").color(NamedTextColor.RED));
                    return true;
                }
                checkPlayer(sender, args[1]);
                break;
            case "checkip":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("用法: /publicban checkip <IP地址>").color(NamedTextColor.RED));
                    return true;
                }
                checkIp(sender, args[1]);
                break;
            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("===== 公共封禁API 命令帮助 =====").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text()
            .append(Component.text("/publicban list").color(NamedTextColor.GOLD))
            .append(Component.text(" - 显示公共封禁列表").color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("/publicban stats").color(NamedTextColor.GOLD))
            .append(Component.text(" - 显示公共封禁统计信息").color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("/publicban check <玩家名>").color(NamedTextColor.GOLD))
            .append(Component.text(" - 检查玩家是否在公共封禁列表中").color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("/publicban checkip <IP地址>").color(NamedTextColor.GOLD))
            .append(Component.text(" - 检查IP是否在公共封禁列表中").color(NamedTextColor.WHITE))
            .build());
    }

    @SuppressWarnings("unchecked")
    private void showBanList(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<String, Object> banData = publicAPIService.getBanData();
                List<Map<String, Object>> activePlayers = (List<Map<String, Object>>) banData.get("active_players");

                sender.sendMessage(Component.text("===== 公共封禁列表 =====").color(NamedTextColor.YELLOW));

                if (activePlayers == null || activePlayers.isEmpty()) {
                    sender.sendMessage(Component.text("当前没有公共封禁记录").color(NamedTextColor.GRAY));
                    return;
                }

                for (Map<String, Object> player : activePlayers) {
                    displayPlayerBanInfo(sender, player);
                }
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "获取公共封禁列表失败: " + e.getMessage());
                plugin.getLogger().warning("获取公共封禁列表时出错: " + e.getMessage());
            }
        });
    }

    private void displayPlayerBanInfo(CommandSender sender, Map<String, Object> player) {
        String username = (String) player.get("username");
        String cause = (String) player.get("cause");
        String ip = (String) player.get("ip");
        String timestamp = formatTime((String) player.get("timestamp"));

        sender.sendMessage(Component.text()
            .append(Component.text("玩家: ").color(NamedTextColor.YELLOW))
            .append(Component.text(username).color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("原因: ").color(NamedTextColor.YELLOW))
            .append(Component.text(cause).color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("IP: ").color(NamedTextColor.YELLOW))
            .append(Component.text(ip).color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("封禁时间: ").color(NamedTextColor.YELLOW))
            .append(Component.text(timestamp).color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text("----------").color(NamedTextColor.GRAY));
    }

    private String formatTime(String isoTime) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
            Date date = isoFormat.parse(isoTime);
            return dateFormat.format(date);
        } catch (Exception e) {
            return isoTime; // 如果解析失败，返回原始字符串
        }
    }

    private void showStats(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<String, Object> stats = publicAPIService.getStats();

                sender.sendMessage(Component.text("===== 公共封禁统计 =====").color(NamedTextColor.YELLOW));

                sender.sendMessage(Component.text()
                    .append(Component.text("封禁玩家数: ").color(NamedTextColor.GOLD))
                    .append(Component.text(stats.get("player_count").toString()).color(NamedTextColor.WHITE))
                    .build());

                sender.sendMessage(Component.text()
                    .append(Component.text("封禁IP数: ").color(NamedTextColor.GOLD))
                    .append(Component.text(stats.get("ip_count").toString()).color(NamedTextColor.WHITE))
                    .build());

            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "获取公共封禁统计信息失败: " + e.getMessage());
                plugin.getLogger().warning("获取公共封禁统计信息时出错: " + e.getMessage());
            }
        });
    }

    private void checkPlayer(CommandSender sender, String playerName) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<String, Object> banInfo = publicAPIService.checkPlayerBan(playerName);

                if (banInfo == null) {
                    sender.sendMessage(Component.text("玩家 " + playerName + " 不在公共封禁列表中").color(NamedTextColor.GREEN));
                    return;
                }

                sender.sendMessage(Component.text("玩家 " + playerName + " 在公共封禁列表中:").color(NamedTextColor.RED));
                displayPlayerBanInfo(sender, banInfo);

            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "检查玩家封禁状态失败: " + e.getMessage());
                plugin.getLogger().warning("检查玩家封禁状态时出错: " + e.getMessage());
            }
        });
    }

    private void checkIp(CommandSender sender, String ip) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<String, Object> banInfo = publicAPIService.checkIpBan(ip);

                if (banInfo == null) {
                    sender.sendMessage(Component.text("IP " + ip + " 不在公共封禁列表中").color(NamedTextColor.GREEN));
                    return;
                }

                sender.sendMessage(Component.text("IP " + ip + " 在公共封禁列表中:").color(NamedTextColor.RED));

                String cause = (String) banInfo.get("cause");
                List<String> players = (List<String>) banInfo.get("players");
                String timestamp = formatTime((String) banInfo.get("timestamp"));

                sender.sendMessage(Component.text()
                    .append(Component.text("原因: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(cause).color(NamedTextColor.WHITE))
                    .build());

                sender.sendMessage(Component.text()
                    .append(Component.text("关联玩家: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(String.join(", ", players)).color(NamedTextColor.WHITE))
                    .build());

                sender.sendMessage(Component.text()
                    .append(Component.text("封禁时间: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(timestamp).color(NamedTextColor.WHITE))
                    .build());

            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "检查IP封禁状态失败: " + e.getMessage());
                plugin.getLogger().warning("检查IP封禁状态时出错: " + e.getMessage());
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("list", "stats", "check", "checkip"));
            return filterCompletions(completions, args[0]);
        }
        return new ArrayList<>();
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        if (input.isEmpty()) {
            return completions;
        }

        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(completion);
            }
        }
        return filtered;
    }
}
