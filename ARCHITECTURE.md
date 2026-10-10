# Endless Adventure 当前技术架构

更新日期：2026-10-07。依据当前工作区源码，包含未提交改动。本文记录实际实现；功能目标见 [DESIGN.md](DESIGN.md)。两份文档在仓库根目录本地维护，被 .gitignore 排除。

## 1. 运行与职责

Java 21、Eclipse 项目、模块 EndlessAdventure，仅依赖标准库与 java.net.http。Driver 创建 UIHandler、SaveManager、StoryGenerator(new LlmClient())，交给 GameEngine.run。工作目录为项目根目录，配置与存档使用相对路径。

以下路径相对 src/com/endlessadventure/：

| 类型／位置 | 当前职责 |
|---|---|
| GameEngine | 单线程 while 主循环、内部 GameScreen、命令路由、角色创建、情境推进、存读与删除 |
| UIHandler | Scanner 行输入、字符界面、换行、清屏及一次性状态消息 |
| GameState | Player、CurrentScene、List<SceneRecord>；默认玩家与情境为 null，历史为空 |
| story/Scene | 抽象基类：地点、描述、回合；回合至少为 1 |
| story/CurrentScene | 当前情境和 1–3 个选项，数组在构造与 getter 时复制；toRecord 保存所选行动 |
| story/SceneRecord | 已完成情境及 playerAction；行动结果体现在下一情境 |
| story/StoryGenerator | 构造上下文、请求模型、验证并返回 CurrentScene，不直接修改 GameState |
| entity/Entity → Combatant → Player | 生命／等级 → 攻击／护甲／能量 → 本级 EXP 与 Inventory；前两层为抽象类型 |
| inventory/Item | 抽象物品，进程内自增 ID、名称与描述；子类须提供 toString、equals、hashCode |
| inventory/Inventory | ArrayList<Item>；添加时拒绝 null，按索引读取／移除，查询数量 |
| save/SaveManager | 三个 Properties 槽位、完整加载生成摘要、写入与删除；内含 SlotStatus、SlotOverview |
| save/BadSaveException | checked exception，保留槽位及可选 cause，描述无效存档状态 |
| save/PropertyNotFoundException | 缺少或空白必需字段的运行时异常 |
| llm/LlmClient、LlmRequestException | 同步 HTTP、配置、API JSON 文本提取；报告缺 key、超时、HTTP 或响应错误 |

Player 初值为 Lv1、HP40、Attack6、Armor1、Energy0/5、EXP0，上限 20 级。已有属性校验与经验表，无自动升级或奖励执行。每个 Player 创建空 Inventory；尚无具体物品子类、装备效果和库存持久化。

## 2. 主循环与状态变化

GameScreen 包含 MAIN_MENU、CHARACTER_CREATION、LOAD_SAVE、WRITE_SAVE、ADVENTURE、INVENTORY、COMBAT、HELP。处理器返回下一屏；退出设置 running=false，输入 EOF 正常结束循环。

- **创建：** New 替换为新 GameState，输入姓名后创建 Player。C 进入冒险，R 清除玩家重新输入，B 回主菜单；不自动保存。
- **开场：** currentScene 为 null 才生成回合 1。已有情境直接显示，读档与导航不重复生成开场。
- **行动：** 优先处理 S 保存与 B 返回；其余输入将合法编号映射为选项原文，否则作为自由行动。空白不发送。生成成功后先替换 currentScene，再追加上一情境与行动至 history；生成器按当前回合 +1 创建新情境。
- **失败：** 开场 LlmRequestException 返回主菜单；续写同类异常留在 ADVENTURE，旧情境和历史不变。无自动重试、离线故事或主菜单 Continue。
- **存档菜单：** 1–3 选择槽位，覆盖非空／损坏槽位须确认。D／del／delete 加槽号删除，须确认。保存成功仍留在存档页；读档成功替换整个 GameState 后进入冒险。
- **占位屏幕：** INVENTORY、COMBAT 处理器直接返回冒险，尚无玩法路由；Help 由主菜单打开。

history 直接以可变 List 暴露，GameState setter 未统一校验完整状态；历史顺序及与回合的关系在写档时检查。生成返回后由引擎提交，没有通用事务系统。

## 3. 模型输入与响应

LlmClient 构造时读取配置：.env 优先于进程环境变量，再使用默认值；显式空 key 禁止请求。同步 HttpClient 连接超时 10 秒、请求超时 60 秒。外层 API JSON 与游戏正文分开解析，HTTP 错误消息遮蔽配置的 key。

请求包含完整 HISTORY 表、续写时的 CURRENT、PLAYER 和新 ACTION。历史每行记录回合、地点、描述、所选行动；制表符、换行、反斜杠转义。玩家资料为姓名、等级、HP；未发送背包或能力评估，尚无摘要或请求长度预算。

正文仅含 location、description、option.count、连续的 option.N.text，**没有协议版本字段**；回合由 Java 指定。每行按第一个等号解析，保留值内等号和字面反斜杠。

| 校验 | 当前规则 |
|---|---|
| 正文 | 非空，最多 8000 字符，重复字段拒绝 |
| 字段／数量 | 必需字段齐全，计数与总字段数相符，索引连续；缺失或额外字段拒绝 |
| 选项 | 至少 1 项，验证所有项后保留前 3 项；保留项清理／截断后忽略大小写去重 |
| 文本 | 规范标点，删除非可打印 ASCII；拒绝控制字符与清理后的空值 |
| 长度 | 地点最多 30，超长拒绝；描述／选项截断到 500／120 |
| 错误 | 正文 IllegalArgumentException 包装为 INVALID_RESPONSE；不自动重试 |

提示词限制为探索剧情，不授予物品、技能、EXP 或改变属性。程序只提交文本情境，不保证叙述语义完全符合规则。

## 4. 存档格式与边界

saves/slot1.txt 至 slot3.txt，UTF-8 Properties；注释为 EndlessAdventure save v2，但没有读取校验的版本键。

| 内容 | 字段 |
|---|---|
| 玩家 | name、level、EXP、hp、maxHp、attack、armor、energy、maxEnergy |
| 当前情境 | location、description、option_count、options.N |
| 历史 | history.count、history.N.location、history.N.description、history.N.action |

不单独保存回合：历史记录按索引 +1 恢复回合，当前回合为 history.count +1。无库存、装备、技能或区域数据。

写档前拒绝 null 当前情境，要求历史数量等于当前回合减一，记录回合依次为 1…N。组装 Properties 后直接打开目标文件写入，没有临时文件或原子替换。

加载要求字符串非空、浮点数有限，Player 校验属性；历史数量非负，选项数在分配数组前检查为 1–3。加载完整的新 GameState 后由引擎替换。读档不复用模型正文的文本限长与清理规则。

槽位摘要执行完整加载：不存在为 EMPTY，成功为 READABLE，捕获的 IO／BadSaveException 为 CORRUPT。缺少 history.count 的旧档视为坏档，没有迁移或专门的版本不兼容提示。删除使用 Files.deleteIfExists。

## 5. 界面与当前限制

Unicode 边框、ANSI 颜色、清屏重绘；当前 UI_WIDTH=204、UI_HEIGHT=48、内边距 2。宽度参与布局，高度未形成分页或裁剪约束。Scanner 输入需 Enter；非 raw 输入转小写，自由行动也经过此路径，姓名保留大小写。

背包渲染接收 Inventory 与 selectedIndex，仅有按 12 格计算页号等占位内容，未绘制可用背包或支持即时方向键。

当前边界：直接覆盖存档，写入中断可能损坏原档；历史、存档和请求无总量上限；帮助显示 back，处理器只接受 b／[b]。没有战斗、休息、专用对话或奖励结算。

## 6. 验证边界

2026-10-07 本次核对中，全部 src 使用 JDK 21 编译通过。仅维护文档，未修改源码、测试或运行脚本。

现有检查为 tests/LlmConfigCheck.java、tests/StoryGeneratorCheck.java，本次未重新执行。未访问真实模型、个人存档或进行交互游玩；编译通过不等于上述路径已验收。
