# Creeper Healing: Sylvan Edition

适用于 Minecraft 26.1.2 的非官方 Fabric 移植版。爆炸后，模组会按设定的延迟逐步恢复被破坏的方块。项目基于 [ArkoSammy12 的 Creeper Healing](https://github.com/ArkoSammy12/creeper-healing) 2.1.4；本仓库由 Sylvan-Cheng 维护，与原作者的正式发行版无关。

## 安装

- Minecraft **26.1.2**、Java **25**、Fabric Loader **0.19.5** 或更新的兼容版本。
- 安装对应 26.1.2 的 Fabric API，以及 Fabric Language Kotlin。
- 从[本仓库的 Releases](https://github.com/Sylvan-Cheng/creeper-healing-sylvan/releases)下载普通 `.jar`，放入游戏或服务端的 `mods` 目录。不要安装名称带 `sources` 的源码包。

从旧版 `creeperhealing_2612` 升级时，先移除旧 JAR，避免两个模组同时处理爆炸。升级世界前建议备份存档和 `config/creeper-healing.toml`。本版沿用原有配置文件名和世界中的 `scheduled-explosions.json`；模组 ID 为 `creeperhealing_sylvan`。

## 功能与配置

默认仅恢复生物造成的爆炸，包括苦力怕爆炸。可在 `config/creeper-healing.toml` 中分别控制生物、TNT、床与末地水晶等爆炸来源是否恢复、是否掉落物品。配置文件在首次启动时生成；拥有管理员权限的玩家也可使用 `/creeper-healing config` 修改设置，或用 `/creeper-healing config reload` 重新加载文件。

常用设置：

| 配置项 | 用途 |
| --- | --- |
| `[delays]` | 设置爆炸开始恢复和逐块恢复的延迟，单位为秒，最小值为 `0.05`。 |
| `[explosion_healing_mode]` | 选择普通、白天、按难度或按方块抗爆性恢复。 |
| `[preferences]` | 控制方块实体数据、下落方块、音效、粒子及药水触发等行为。 |
| `[whitelist]` | 开启后只恢复列出的方块；开关是 `[preferences]` 中的 `enable_whitelist`。 |
| `[replace_map]` | 指定恢复时用哪种方块替代原方块。 |

恢复模式的配置值依次为 `default_mode`、`daytime_healing_mode`、`difficulty_based_healing_mode` 和 `blast_resistance_based_healing_mode`。抗爆性模式包含随机延迟，方块可能成批恢复。

## 此移植版的行为

- 普通模式按方块依次等待和恢复。重启后保留已保存的模式专用方块延迟。
- 白天模式在首次恢复前若所有受影响位置都没有光，会继续等待并定期检查光照；上游版本会在这时结束该任务。
- 暂时无法放置的方块会留在待恢复任务中，之后继续重试，没有固定次数上限。若玩家先在原位置建造，之后又拆除，新方块可能被旧任务的方块取代。目前没有取消单个待恢复任务的游戏内命令。关闭某类爆炸的恢复只影响此后发生的爆炸。
- 待恢复任务会在正常停服时保存，运行中也会定期保存进度；异常退出仍可能丢失最近一次成功保存后的进度。

这些行为与[上游基线提交 `4fcf0a0`](https://github.com/ArkoSammy12/creeper-healing/commit/4fcf0a002218dd6d2080f6c1c413a81c537897c1)有差异。完整版本记录见 [CHANGELOG.md](CHANGELOG.md)。

## 构建与反馈

安装 Java 25 并设置 `JAVA_HOME` 后，在仓库根目录运行 `./gradlew build`；Windows PowerShell 可运行 `./gradlew.bat build`。普通构建产物位于 `build/libs/`。移植版问题请提交到[本仓库的 Issues](https://github.com/Sylvan-Cheng/creeper-healing-sylvan/issues)，上游项目的问题请提交到上游仓库。

本移植版以 [LGPL-3.0-only](LICENSE) 发布；相关 GPL 条款见 [COPYING.GPLv3](COPYING.GPLv3)。项目包含按 [MIT 许可](MONKEY-UTILS-LICENSE)改编的 Monkey Utils 源码。原模组作者为 [ArkoSammy12](https://github.com/ArkoSammy12)，模组图标由 [Kioku](https://github.com/takoyakioku) 制作。
