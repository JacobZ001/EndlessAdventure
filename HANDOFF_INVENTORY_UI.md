# Endless Adventure：Inventory 界面任务交接

交接日期：2026-10-08（Asia/Dubai）  
项目目录：`C:\Users\jacob\cmp358\EndlessAdventure`  
状态：应用户要求停止推进开发；本次仅检查当前状态并编写交接文件。

## 1. 新会话首先需要知道的事

当前任务是帮助 Jacob **亲手实现带 Unicode 彩色图片和即时方向键选择的 Inventory 界面**。用户已否定上一版 ADVENTURE 界面设计，并明确把 ADVENTURE 重设计推迟，先完成 Inventory。

最近一轮只完成了源码检查、参考素材定位和实施方案讲解。**尚未接入 JLine、创建 ConsoleCanvas、制作正式背包界面或修改游戏核心代码。** 不能把聊天中的代码示例、外部视觉原型或建议当作已集成成果。

用户最新要求是：“接下来不再继续执行，把当前进行中的任务状态和未完成规划总结成一份交接文件，用于迁移任务至新会话”。本文件完成后停止工作；新会话按用户后续指令继续。

## 2. 授权与协作边界

先读取项目根目录 `AGENTS.md`，以其中最新规则为准。关键约束：

- 这是 Jacob Zhou 的 CMP358 Java 课程项目，主要目标是学生亲手编写并理解程序。默认提供设计、解释、审阅、验证；**不能因发现问题就直接修改非 LLM 代码**。
- 用户上一轮问“这个界面要怎么实现”，属于教学与设计请求，没有授权代理实现整个 Inventory、JLine 输入或画布。可在聊天中给聚焦的示例，不自动写入项目。
- LLM 例外覆盖 `src/com/endlessadventure/llm/` 与 `story/StoryGenerator.java` 中的请求、提示词、解析、校验；不自动覆盖调用它的游戏流程，也不覆盖 Scene、UI、背包、存档。
- 不用新建测试、原型、启动脚本来绕过课程代码限制。以前针对其他任务的代码修改授权不自动延续到本任务。
- 文档更新不附带代码重构。`DESIGN.md` 记录目标和状态，`ARCHITECTURE.md` 只描述已存在的实现；本次没有更新这两份文件。
- 保护未提交修改；不自动回撤、提交、推送。`AGENTS.md` 被忽略，禁止提交到 public repo。本交接文件包含本机路径，供本地迁移使用，不默认发布。

用户沟通偏好：中文、结论优先、具体判断、解释清楚但避免碎片化和重复。不大量分标题，不在常规解释中使用“不是……而是……”。用户重视学习过程，不以代理快速完成替代教学。

## 3. 项目背景与当前优先级

Endless Adventure 是 Java 21 / Eclipse / Windows Terminal 的 console RPG 课程项目。此前已提交 proposal，当前在开发阶段。已确认无限冒险、离散回合制，LLM 生成剧情和挑战，Java 管理权威游戏状态。本项目也用于探索 Vestige 尚未充分涉及的传统 RPG 属性、物品、装备等玩法；当前无需重做 Vestige 架构讨论。

本阶段优先级：Inventory 的可操作界面和真实状态连接。ADVENTURE 新视觉暂缓；不要继续推荐被用户否定的“四区冒险屏”作为当前方案。

用户主要在 Windows Terminal 中运行游戏。此前提供的设置是 Cascadia Mono、字号 12、内置块字符绘制开启；用户实际测得最大化窗口为 **209 列 × 51 行**。这是那套字体、缩放和窗口条件下的观测值，不能泛化成所有 1080p 屏幕的固定字符数。本任务以固定布局为目标，无需先做完整自适应系统。

## 4. 当前源码状态：交接时只读核对

核对时间为 2026-10-08；以下是源码检查结论，**没有在本次交接中编译、运行测试或实际游玩**。

| 文件 / 类型 | 当前状态 |
|---|---|
| `.classpath` | JavaSE-21、src、bin；没有 JLine 条目 |
| `src/module-info.java` | 模块名 `EndlessAdventure`，目前仅 `requires java.net.http` |
| `lib` | 交接检查时不存在 |
| `UIHandler` | `UI_WIDTH = 204`、`UI_HEIGHT = 48`、`TEXT_PADDING = 2`、`INV_DIMENSIONS = {3,4}`；使用 `Scanner(System.in)` |
| `UIHandler.prompt()` | 行输入，需要 Enter；非 raw 路径会转小写 |
| `UIHandler.render()` | 清屏后输出内容、额外说明和状态行；还不是固定高度画布输出 |
| `UIHandler.renderInventoryUI(Inventory, int)` | 只有 `page = selectedIndex / 12`、局部 `itemIndex` 声明和 TODO，尚无可用界面 |
| `GameEngine.handleInventory()` | TODO，立即返回 ADVENTURE |
| `GameEngine.handleAdventure()` | 仍有待实现 Inventory 命令的 TODO，未接通背包入口 |
| `inventory.Inventory` | `ArrayList<Item>`；支持 addItem、getItem、removeItem、getSize；拒绝添加 null |
| `inventory.Item` | 抽象类；进程内自增 ID、name、description；要求子类实现 toString、hashCode、equals；尚无具体物品子类 |
| `entity.Player` | 持有 final Inventory；有基础 HP、等级、Attack、Armor、Energy、EXP；未有正式装备槽和装备效果系统 |
| `save.SaveManager` | 已有一般存档逻辑；背包写入、读取各有 TODO |

`GameEngine` 已有屏幕枚举与处理器循环；Inventory 应接入该流程，避免另起一个与正式游戏状态脱节的入口。

交接前工作区有这些未提交修改：

```text
 M src/com/endlessadventure/GameEngine.java
 M src/com/endlessadventure/UIHandler.java
```

这些在创建本交接文件之前就已存在，不能归为本次代理修改，也不要覆盖。Git 曾报告目录所有者不一致；本次仅用命令级 `git -c safe.directory=C:/Users/jacob/cmp358/EndlessAdventure status --short` 读取状态，没有修改全局 Git 配置。

## 5. 用户明确提出的视觉与交互目标

用户提供的 Inventory 效果图包含：

- 左栏：人物立绘、基础属性、主手 / 胸甲 / 饰品装备槽。
- 中栏：4×3 背包格子、彩色道具图标、名称、数量或稀有度标识、顶部分类、底部页码。
- 右栏：当前选中物品的大图、说明、属性和与已装备物品的比较。
- 底部：方向键选择、分类、排序、退出等快捷操作提示。

用户明确希望使用 **JLine 直接捕捉方向键，无需每次 Enter**。图片来自 Cloudflare Workers AI 生成后转换为 Unicode 字符的素材流程。

效果图表达了用户的目标外观，但其中 24 格容量、堆叠数量、金币、负重、稀有度、抗性、角色职业与具体装备数值，不等于已经确定的玩法规则或已经实现的字段。应区分“要接近该外观”和“把全部样例系统纳入课程 scope”。

## 6. 已找到的参考原型与素材

参考图所在目录：

```text
C:\Users\jacob\.codex\visualizations\2026\10\02\01a0fcda-f14e-7b60-be0b-d621f65078ff\unicode-inventory-api\character-ui
```

该目录已在本会话只读检查，关键文件：

| 文件 | 用途 / 限制 |
|---|---|
| `README.md` | 原型说明、尺寸、素材来源和验证范围 |
| `inventory-character-209x51.png` | 用户参考图对应的字符画布模拟图片；不是 Windows Terminal 截图；图上方说明文字在 51 行画布之外 |
| `inventory-character-209x51.ans` | 对应的 ANSI 输出 |
| `inventory_character.py` | 固定 209×51 布局和预览交互的参考实现；独立于课程游戏 |
| `source-ranger.png` / `prepared-ranger.png` | 人物原图和预处理图 |
| `ranger.json` | 人物转换结果，44×32 字符格，对应 44×64 颜色采样点 |
| `PlayerVariant.java` | 调用已有 AsciiStudy 转换器导出人物数据的参考 |
| `preview.cmd` | 外部预览入口，不是游戏启动器 |

同一父目录内还有道具原图、`prepared`、`output` 和 `grid-ui`。此前已找到药水、剑、盾、钢甲、附魔甲、法杖和戒指素材。优先复用并理解现有资产，不为方向键操作重新生成图片。

`grid-ui` 是另一个自适应终端原型，`character-ui` 是当前参考的固定布局。不要混淆两者的尺寸和验证记录。

原型限制：只有展示、选择、筛选和排序预览；没有正式装备、游戏状态变更或真实容量规则。固定原型中的 24 格只是设计参考，未实现分页；立绘没有与装备槽动态同步。旧原型的测试通过不能作为当前 Java 游戏验证结果。

上述目录在本次环境中可以读取，但不在本次项目可写根目录内。不要为了迁移说明去改动原型。父目录可能有 API 凭据文件，不读取或回显凭据，不打包或提交它们。

用户附件临时路径，若已过期可改用上方持久化 PNG：

```text
C:\Users\jacob\AppData\Local\Temp\codex-clipboard-128d255d-59e3-40f6-8f51-8074e9d7c96c.png
```

## 7. 上一轮提出的实施方案：建议，尚未获逐项确认或实现

### 7.1 固定布局

建议先依照参考图为 Inventory 使用 209×51 的画布，其他旧屏幕暂时保留现状。现有全局常量是 204×48；两套尺寸如何统一仍需在实际实现时明确，不能混用。

参考原型坐标从 0 开始：

| 区域 | `(x, y)` | 宽 × 高，含边框 |
|---|---|---|
| 人物与装备 | `(3, 9)` | `65×37` |
| 背包 | `(72, 9)` | `89×37` |
| 物品详情 | `(165, 9)` | `41×37` |

网格 4 列 × 3 行，共用格子边框；横向步长 22 列，纵向步长 12 行。单格含共享边界时为 23×13。使用细线、深色统一背景、金色选择边框；图标保持原色。此方案替换装饰性较重的旧边框风格，但尚未改旧组件。

### 7.2 一个小型字符画布

最近建议使用 `ConsoleCanvas` 之类的小类，保存 `char[][] characters`、`int[][] foreground`、`int[][] background`，坐标 `(x,y)` 映射到 `[y][x]`。最初可提供填背景、写文字、画线/矩形、贴图、输出一帧。绘制时检查边界，文字按区域换行或裁剪。

ANSI 控制序列只在最终输出阶段生成，不放进坐标数组参与排版。先支持英文、框线和单列块字符，不宣称 `char` 或 String.length 能通用处理所有 Unicode 显示宽度。

此前为简单文本面板建议过固定宽度 `String[]`；这次因为用户指定了彩色立绘、网格与边框叠加，才提出坐标画布。不要无条件重构所有屏幕或开发通用 UI 框架。

### 7.3 图片表示与载入

用 `▀` 的前景色表示上半像素，背景色表示下半像素；输出用 `38;2;r;g;b` / `48;2;r;g;b` RGB 序列。16×16 颜色采样点可放入 16×8 字符格；现成背包图标采用这一尺寸，人物为 44×32 字符格。

建议预先生成、去除多余背景、保持比例缩小、转换并保存。游戏加载时读取和缓存，重绘时复制已有数据。小图标与大预览分别从原图采样，避免从小图放大。JSON、预生成字符资源或其他具体资源格式尚未决定；不要为此未经讨论新增解析库。

图片背景要与画布一致；绘制后恢复正确的前景/背景，避免颜色泄漏到边框和文字。人物先用固定立绘，动态换装立绘不属于首个可用版本。

### 7.4 JLine 输入和终端生命周期

Java 21 建议使用 JNI provider。此前说明过手动 Modulepath 依赖路线：同一版本的 `jline-terminal`、`jline-reader`、`jline-terminal-jni` 和所需 `jline-native`。**尚未选定或安装具体版本，也没有验证 Eclipse 配置或启动命令**。新会话实施时应按实际版本核对依赖与模块名，不盲用不同版本教程。Java 22+ 的 FFM 路线不是当前 Java 21 的默认选择。

建议全程序共用一个 Terminal：保留 UIHandler.prompt 的调用入口，内部逐步切换为 LineReader.readLine；Inventory 使用 raw mode 和 BindingReader / KeyMap。避免 Scanner 与 JLine 独立读取、缓冲同一输入流。

进入背包时保存终端属性、进入 raw mode、设置 keypad_xmit、使用备用屏幕并隐藏光标；退出和异常路径恢复原属性、keypad_local、颜色、光标与原屏幕。处理 Esc、EOF 和退出路径；Esc 与方向键共享转义前缀，可用 KeyMap 的短歧义等待识别。不要把整套配置误当成上一轮短代码片段已经实现。

可以同步等待一个按键后更新，不需要用户编写线程或固定 FPS 循环。若以后需要窗口尺寸监测再增加针对性处理；当前固定布局也应明确最小窗口需求。

Windows Terminal 启动历史上曾因 `bin;lib` 的分号被 wt 当作命令分隔符而失败。添加依赖时要验证完整 Eclipse → cmd → wt → java 的传参，不能只确认 Java 命令本身正确。

### 7.5 选择、分页、数据所有权

建议选择状态放在背包交互逻辑中，由 UI 根据数据绘制。初期选择只在背包网格内移动，装备槽仅展示当前装备，详情自动随选中物品更新。

对当前展示的物品列表，非空时可使用：

```text
page = selectedIndex / 12
localIndex = selectedIndex % 12
row = localIndex / 4
column = localIndex % 4
```

左右移动 ±1，但不越过当前行边界；上下 ±4，目标存在才移动，跨页后显示新页。空背包没有有效选中物品，不直接套公式或 getItem。Item.id 不是索引。筛选、排序后应根据展示列表取实际 Item，并重新确认选择有效；删除、使用完物品时也需要修正索引。

参考图上的总容量 24、是否允许选空格、是否额外提供翻页键、堆叠规则都尚未正式定案。不要将任意样例数值写死成已确认规则。

### 7.6 绘制和刷新

建议库存模式覆盖固定位置，每次有效按键导致状态变化时重画一帧；不用当前每轮清屏、不断 println 的通用 render 流程。行尾补背景、处理短文本覆盖长文本、避免最后一行额外换行导致滚屏。坐标输出时明确 ANSI 从 1 开始、数组从 0 开始的转换。

第一版无需动画或自行开发差异渲染；若实际出现性能/闪烁问题，再考虑 JLine Display。不能声称此方式已在当前 Java 项目实测。

### 7.7 接入现有代码

- UIHandler：渲染和输入设施；现有 renderInventoryUI 只收到 Inventory，展示人物和装备时建议改为传 Player 加选择状态。尚未改签名。
- GameEngine.handleInventory：维护交互循环，处理移动、退出和物品操作；handleAdventure 增加背包入口。
- Inventory：真实物品集合。显示排序/筛选视图与实际库存操作明确区分。
- Player 与 Item 子类：装备状态、使用/装备规则以及实际属性；不由绘制函数修改游戏状态。
- SaveManager：真实物品和装备持久化需另行完成，目前有 TODO。

装备/背包的所有权规则尚未确定：装备是从背包移出还是仍在列表中标记装备、如何替换旧装备、如何计算最终属性。接入真实装备操作前需要明确，不能只把右栏数值画出来就认为装备系统完成。

## 8. 未完成工作与建议继续顺序

以下为实施候选顺序；不构成代理自动代写的授权。

1. **输入与可操作浏览闭环**：确定 JLine 版本及 Eclipse Modulepath 配置；教用户建立 Terminal 生命周期，接通 Inventory 入口、方向键选择、详情同步、Esc 返回。先用少量合法物品数据。检查返回普通输入后仍能正常输入文字。
2. **还原目标视觉**：逐步编写画布、框线、网格，再接入现有图标和人物数据；固定三栏布局、背景、选择高亮与重绘。使用真实终端验证，不只生成 PNG 模拟图。
3. **接入真实物品操作**：按用户确认的范围实现具体 Item 子类、装备/使用、分类与排序；随后完成存档恢复。金币、负重、抗性、动态立绘等是否纳入本次仍待决定。

建议阶段性验证：空背包、1/12/13 件物品、行边界、跨页、筛选无结果、移除最后一件、长名称、图片颜色与格子对齐、按键无需 Enter、Esc 不误判方向键、恢复原屏幕和普通输入。若未来获准实现，检查应针对实际逻辑，不复制实现写无意义测试。

## 9. 参考资料与新会话启动方式

本会话上轮查询过的官方资料（本次交接没有再次联网验证版本）：

- JLine 入门：<https://jline.org/docs/intro/>
- JLine 模块与 JPMS：<https://jline.org/docs/modules/jpms/>
- BindingReader 与 keypad 模式：<https://jline.org/docs/advanced/non-blocking-input/>
- 备用屏幕、raw mode、Display：<https://jline.org/docs/advanced/screen-clearing/>
- Windows ANSI 控制与 RGB：<https://learn.microsoft.com/en-us/windows/console/console-virtual-terminal-sequences>

新会话建议依次读取本文件、项目 AGENTS.md、当前 UIHandler / GameEngine / Inventory / Item / Player，再按用户选择的下一步提供教学或执行明确获授权的修改。必要时查看 character-ui 的参考图与源码；无需从课程 proposal、GitHub 建库或被搁置的 ADVENTURE 设计重新开始。

本次交接交付只新增这份 Markdown 文件；未改源代码、依赖、启动配置、DESIGN.md、ARCHITECTURE.md 或记忆文件，未执行 commit/push，也未调用图片 API。
