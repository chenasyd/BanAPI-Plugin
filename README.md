# BanAPI-Plugin 

## 概述
BanAPI-Plugin 是与 BanAPIService 配套的 Paper/Spigot 插件，提供封禁管理功能和跨服支持。

## 版本功能

### banapi-1.0-SNAPSHOT
- **主指令**: `/banapi`
- **功能列表**:
  - `list` - 显示所有封禁记录
  - `stats` - 显示统计信息
  - `ban <玩家名> <原因> <管理员> [永久(true/false)] [时长(毫秒)]` - 添加新封禁
  - `release` - 解封玩家
- **新增特性**:
  - MySQL 跨服提示和公告功能
  - 配置支持

### banapi-1.1-SNAPSHOT
- **主指令**: `/banapi` 新增 `publicban` 子命令
- **新增功能**:
  - `list` - 显示公共封禁列表
  - `stats` - 显示统计信息
  - `check <玩家名>` - 检查玩家是否存在于公共封禁列表中
  - `checkip <ip地址>` - 检查IP是否存在于公共封禁列表中

### banapi-1.2-SNAPSHOT
- **新增特性**:
  - 增加了 BungeeCord 的显示支持

## 使用说明

### 基本命令格式
```
/banapi [子命令] [参数]
```

### 示例用法
1. 封禁玩家:
```
/banapi ban Notch 使用外挂 Admin true
```

2. 检查公共封禁:
```
/banapi publicban check Herobrine
```

3. 检查IP封禁:
```
/banapi publicban checkip 127.0.0.1
```

## 注意事项
- 使用前请确保已正确配置BanAPIService
- 1.2版本及以上需要BungeeCord环境支持跨服功能
- 永久封禁参数为可选，默认为false

如需更多帮助，请参考插件内帮助命令或联系开发者。
