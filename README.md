# Rollback Mod（NeoForge 1.21.x 移植版）

这是 Rollback Mod 从 1.20 Forge 向 **NeoForge 1.21.x** 的移植：
玩法与优化版完全一致（存档覆盖回溯、轻量化药芯（化茧/蜕皮/高塔）、单格吸入器、
滚动天数 HUD、多人生命/背包统一、hardcore 式毁灭等），并针对 1.21.1 做了原生适配：

- 事件体系迁移：`LivingHurtEvent` → `LivingDamageEvent.Pre`（不可取消，用 `setNewDamage(0)`），
  `NeoForge.EVENT_BUS`。
- 注册体系迁移：`DeferredRegister`/`DeferredHolder`。
- NBT API 适配：序列化携带 `HolderLookup.Provider`（1.21 新签名）；物品数据用 `DataComponents.CUSTOM_DATA`。
- 网络：`CustomPacketPayload` + `StreamCodec`；`DayInfoPacket` 通过 `RegisterPayloadHandlersEvent` 注册。
- HUD：`RegisterGuiLayersEvent`，黑屏层用 `registerAboveAll` 注册在最上层（遮挡聊天栏/消息栏）。
- 配置：JSON 文件 `config/rollbackmod.json` + **Cloth Config**（`cloth-config-neoforge`，按版本匹配，中英双语）。

## 构建

- 需要 JDK 21（Gradle 8.12 由 wrapper 自动下载）。
- 已包含 Gradle Wrapper：`gradlew.bat build -Pmc=<版本>`（Linux/macOS：`./gradlew build -Pmc=<版本>`）
  - 不传 `-Pmc` 则构建默认版本 1.21.1；`<版本>` 取 `versions/` 下文件名（如 `1.21`、`1.21.11`）。
- 产物：`build/libs/rollbackmod-1.1.0-<版本>-neoforge.jar`。
- 本仓库一套源码覆盖 NeoForge **1.21.x**（1.21 – 1.21.11）；各版本的差异在 `versions/<版本>.properties`
  与 `src/versioned/<版本>/`，无需为每个小版本另开仓库。

## 与 1.20.1 Forge 版的差异

| 项 | 1.20.1 Forge | 1.21.1 NeoForge |
| --- | --- | --- |
| 伤害事件 | LivingHurtEvent | LivingDamageEvent.Pre（不可取消） |
| HUD 注册 | RegisterGuiOverlaysEvent | RegisterGuiLayersEvent |
| 物品数据 | ItemStack NBT | DataComponents.CUSTOM_DATA |
| 配置 | Cloth Config 11.x | Cloth Config 15.x |
| 物品堆耐久 | hurtAndBreak(int, entity, cb) | hurtAndBreak(int, level, entity, cb) |

致谢：作者 HanMoyun；朋友 @liyuu、@LAST-iMP。
