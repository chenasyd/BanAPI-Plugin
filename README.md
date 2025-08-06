# BanAPI-Plugin
与BanAPIService配套的paper spigot插件

banapi-1.0-SNAPSHOT
主指令/banapi
list -显示所有封禁
stats -显示统计信息
ban <玩家名> <原因> <管理员> [永久（true/false）] [时长（毫秒）] -添加新封禁
release -解封
增加了MySQL的跨服提示和公告及其配置支持

banapi-1.1-SNAPSHOT
主指令/banapi：publicban
list -显示公共列表
stats -显示统计信息
check <玩家名> -检查玩家是否存在于公共封禁列表中
checkip <ip地址> -检查IP是否存在于公共封禁列表中

banapi-1.2-SNAPSHOT
增加了bungee的显示支持
