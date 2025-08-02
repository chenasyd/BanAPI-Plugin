package org.a.banapi.commands;

import org.a.banapi.Banapi;
import org.a.banapi.api.APIService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 处理BanAPI命令，用于查询和操作API内容
 */
public class BanAPICommand implements CommandExecutor, TabCompleter {
    private final Banapi plugin;
    private final APIService apiService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public BanAPICommand(Banapi plugin) {
        this.plugin = plugin;
        this.apiService = plugin.getApiService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("banapi.admin")) {
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
            case "ban":
                if (args.length < 4) {
                    sender.sendMessage(Component.text("用法: /banapi ban <玩家名> <原因> <管理员> [永久(true/false)] [时长(毫秒)]").color(NamedTextColor.RED));
                    return true;
                }
                handleBan(sender, args);
                break;
            case "release":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法: /banapi release <ID>");
                    return true;
                }
                handleRelease(sender, args);
                break;
            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("===== BanAPI 命令帮助 =====").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text()
            .append(Component.text("/banapi list").color(NamedTextColor.GOLD))
            .append(Component.text(" - 显示所有封禁列表").color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("/banapi stats").color(NamedTextColor.GOLD))
            .append(Component.text(" - 显示封禁统计信息").color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("/banapi ban <玩家名> <原因> <管理员> [永久(true/false)] [时长(毫秒)]").color(NamedTextColor.GOLD))
            .append(Component.text(" - 添加封禁记录").color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("/banapi release <ID>").color(NamedTextColor.GOLD))
            .append(Component.text(" - 解除指定ID的封禁").color(NamedTextColor.WHITE))
            .build());
    }

    private void showBanList(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<Map<String, Object>> bansList = apiService.getBans();
                
                sender.sendMessage(Component.text("===== 封禁列表 =====").color(NamedTextColor.YELLOW));
                
                if (bansList.isEmpty()) {
                    sender.sendMessage(Component.text("当前没有封禁记录").color(NamedTextColor.GRAY));
                    return;
                }
                
                for (Map<String, Object> ban : bansList) {
                    displayBanInfo(sender, ban);
                }
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + "获取封禁列表失败: " + e.getMessage());
                plugin.getLogger().warning("获取封禁列表时出错: " + e.getMessage());
            }
        });
    }

    private void displayBanInfo(CommandSender sender, Map<String, Object> ban) {
        int id = ((Number) ban.get("id")).intValue();
        String nickname = (String) ban.get("nickname");
        String reason = (String) ban.get("reason");
        String admin = (String) ban.get("admin");
        boolean isPermanent = (boolean) ban.get("isPermanent");
        boolean isReleased = (boolean) ban.get("isReleased");
        String startTime = formatTime((String) ban.get("startTime"));
        String endTime = ban.get("endTime") != null ? formatTime((String) ban.get("endTime")) : "永久";
        
        sender.sendMessage(Component.text()
            .append(Component.text("ID: ").color(NamedTextColor.YELLOW))
            .append(Component.text(id).color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("玩家: ").color(NamedTextColor.YELLOW))
            .append(Component.text(nickname).color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("原因: ").color(NamedTextColor.YELLOW))
            .append(Component.text(reason).color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("管理员: ").color(NamedTextColor.YELLOW))
            .append(Component.text(admin).color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("类型: ").color(NamedTextColor.YELLOW))
            .append(Component.text(isPermanent ? "永久" : "临时").color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("状态: ").color(NamedTextColor.YELLOW))
            .append(Component.text(isReleased ? "已解除" : "生效中").color(isReleased ? NamedTextColor.GREEN : NamedTextColor.RED))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("开始时间: ").color(NamedTextColor.YELLOW))
            .append(Component.text(startTime).color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("结束时间: ").color(NamedTextColor.YELLOW))
            .append(Component.text(endTime).color(NamedTextColor.WHITE))
            .build());
        sender.sendMessage(Component.text("----------").color(NamedTextColor.GRAY));
    }

    private String formatTime(String isoTime) {
        try {
            // 将ISO 8601格式转换为更易读的格式
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoTime);
            return dateFormat.format(date);
        } catch (Exception e) {
            return isoTime; // 如果解析失败，返回原始字符串
        }
    }

    private void showStats(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<String, Object> stats = apiService.getStats();
                
                sender.sendMessage(Component.text("===== 封禁统计 =====").color(NamedTextColor.YELLOW));
                
                if (stats.containsKey("total")) {
                    sender.sendMessage(Component.text()
                        .append(Component.text("总封禁数: ").color(NamedTextColor.GOLD))
                        .append(Component.text(stats.get("total").toString()).color(NamedTextColor.WHITE))
                        .build());
                }
                
                if (stats.containsKey("active")) {
                    sender.sendMessage(Component.text()
                        .append(Component.text("活跃封禁: ").color(NamedTextColor.GOLD))
                        .append(Component.text(stats.get("active").toString()).color(NamedTextColor.WHITE))
                        .build());
                }
                
                if (stats.containsKey("released")) {
                    sender.sendMessage(Component.text()
                        .append(Component.text("已解除封禁: ").color(NamedTextColor.GOLD))
                        .append(Component.text(stats.get("released").toString()).color(NamedTextColor.WHITE))
                        .build());
                }
                
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + "获取统计信息失败: " + e.getMessage());
                plugin.getLogger().warning("获取统计信息时出错: " + e.getMessage());
            }
        });
    }

    private void handleBan(CommandSender sender, String[] args) {
        String nickname = args[1];
        String reason = args[2];
        String admin = args[3];
        boolean isPermanent = args.length > 4 ? Boolean.parseBoolean(args[4]) : true;
        Long duration = null;
        
        if (!isPermanent && args.length > 5) {
            try {
                duration = Long.parseLong(args[5]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "时长必须是一个有效的数字（毫秒）");
                return;
            }
        }
        
        final Long finalDuration = duration;
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<String, Object> result = apiService.addBan(nickname, reason, admin, isPermanent, finalDuration);
                
                sender.sendMessage(Component.text("成功添加封禁记录：").color(NamedTextColor.GREEN));
                displayBanInfo(sender, result);
                
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + "添加封禁记录失败: " + e.getMessage());
                plugin.getLogger().warning("添加封禁记录时出错: " + e.getMessage());
            }
        });
    }

    private void handleRelease(CommandSender sender, String[] args) {
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "ID必须是一个有效的数字");
            return;
        }
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<String, Object> result = apiService.updateBanStatus(id, true);
                
                sender.sendMessage(Component.text("成功解除封禁：").color(NamedTextColor.GREEN));
                displayBanInfo(sender, result);
                
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + "解除封禁失败: " + e.getMessage());
                plugin.getLogger().warning("解除封禁时出错: " + e.getMessage());
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("list", "stats", "ban", "release"));
            return filterCompletions(completions, args[0]);
        } else if (args.length == 5 && args[0].equalsIgnoreCase("ban")) {
            return Arrays.asList("true", "false");
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
