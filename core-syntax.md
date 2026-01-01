# Fluxon 核心语法速查

本文件只描述 “核心语法形态与约束”，尽量用占位符表达形式；可运行示例请移步 core-syntax.fs。

## 文件与语句

- 脚本通常使用 `.fs` 扩展名。
- 语句之间可用换行或 `;` 分隔（解析器会用 token 行号判断语句边界）。
- 注释：
  - 行注释：`// ...`
  - 块注释：`/* ... */`
- 代码块：`{ <stmt>* <expr>? }`
  - 块的 “值” 通常为最后一个表达式的结果；也可用 `return` 提前返回（在函数体内或顶层）。

## 变量与引用

- 赋值/更新：`name = <expr>`
- 复合赋值：`name += <expr>`、`name -= <expr>`、`name *= <expr>`、`name /= <expr>`、`name %= <expr>`
- 读取变量值（显式引用运算符）：
  - `&name`：严格引用；变量未定义时会报错（除非宿主开启 allowInvalidReference）
  - `&?name`：可选引用；变量未定义或为 `null` 时得到 `null`

关键点：

- **裸标识符在表达式位置默认是字符串字面量**，不是变量引用；要取变量值必须写 `&name`。
- 标识符支持中文；支持 `-` 作为非首字符（例如 `log-level`）。

## 字面量与集合

- 数字：`<int>`、`<long>L`、`<float>f`、`<double>`、科学计数法如 `2.5e3`
- 字符串：`"..."` 或 `'...'`（支持常见转义）
- 布尔/空：`true`、`false`、`null`
- 列表：`[<expr>, <expr>, ...]`
- 映射：`[<key>: <expr>, <key>: <expr>, ...]`（key 可为字符串或标识符文本）

## 运算符（按常见优先级从高到低）

- 后缀：函数调用 `f(...)`、索引访问 `x[<i>]`
- 成员访问（反射）：`obj.member`、`obj.method(...)`
- 上下文调用（扩展函数）：`target :: extFunc(...)` 或 `target :: { ... }`
- 一元：`!<expr>`、`-<expr>`、`await <expr>`、`&name`/`&?name`
- 乘除模：`*`、`/`、`%`
- 加减：`+`、`-`
- 区间：`a..b`（闭区间）、`a..<b`（左闭右开）
- 比较：`> >= < <= == !=`
- 逻辑：`&&`、`||`（短路）
- 三元：`<cond> ? <then> : <else>`
- Elvis：`<expr> ?: <fallback>`（左侧为 `null` 时取右侧）
- 赋值：`=`、`+=` 等

## 控制流（均为表达式）

- `if`：
  - 形式：`if <cond> then <expr> else <expr>`
  - `then` 关键字可省略；分支可用块：`if <cond> { ... } else { ... }`
- `when`：
  - 条件模式：`when { <cond> -> <expr>; ...; else -> <expr> }`
  - 值匹配：`when <value> { <case> -> <expr>; in <collectionOrRange> -> <expr>; else -> <expr> }`
- 循环：
  - `for <name> in <iterable> { ... }`
  - `for (<a>, <b>, ...) in <iterable> { ... }`（解构迭代项）
  - `while <cond> { ... }`
  - `break` / `continue` 仅在循环体内有效

## 异常

- 抛出：`throw(<expr>)`
- 捕获：`try <exprOrBlock> catch <exprOrBlock>`
- `catch` 可带变量：`catch (e) { ... }`
- `finally` 可选：`try { ... } catch { ... } finally { ... }`

## 函数与 Lambda

- 定义函数（仅顶层）：`def name(<params>) = <exprOrBlock>`
  - `=` 可省略：`def name(<params>) { ... }`
  - 参数也可省略括号：`def name a, b { ... }`
  - `async def` / `sync def`：异步/主线程语义（需要宿主运行时支持）
- 调用：`name(<args>)`
- Lambda：
  - `|x| <exprOrBlock>`
  - `|a, b| <exprOrBlock>`
  - `|| <exprOrBlock>`（隐式参数 `it`，自动绑定第一个实参）

注意（验证脚本时很重要）：

- 当前运行时的 `ThreadPoolManager` 线程为非 daemon；只要触发 `async def`/`await`，一次性 CLI 运行可能不会自动退出（需要宿主主动 shutdown 线程池，或避免在 “脚本文件模式” 里演示 async）。

## 上下文调用 `::`（扩展函数）

- 形式：
  - `<targetExpr> :: <extensionFunctionCall>`
  - `<targetExpr> :: { <stmts> }`（块内的调用共享同一 target）
- 语法糖：当左侧是标识符且紧跟 `::` 时，允许省略 `()`：`time :: formatTimestamp(0L)` 等价于 `time() :: ...`
- 命名空间函数/扩展函数通常需要先 `import '<namespace>'` 才能在解析期可见

## 成员访问 `.`

- `.` 基于 Java 反射，通常默认禁用（安全/性能考虑）。
- console/REPL 默认启用；宿主集成时需显式开启 `allowReflectionAccess`。
- 特殊处理：当 target 是 `Class` 对象时（如 `forName("...")` 返回），优先查找该类的静态方法，找不到时回退到 `java.lang.Class` 的实例方法。

## 静态成员访问 `static`

- 形式：`static <fully.qualified.ClassName>.<member>`
- 静态方法调用：`static java.lang.Integer.parseInt("42")`
- 静态字段访问：`static java.lang.System.out`
- 链式调用：`static java.lang.System.out.println("hello")`
- 括号语法消除歧义：`static (ClassName).field.method()`
  - 当需要访问静态字段后继续链式调用时，用括号包裹类名
  - 示例：`static (com.example.MyObject).INSTANCE.doSomething()`
- 需要宿主启用 `allowJavaConstruction` 特性。

## 解构赋值

- 形式：`(<a>, <b>, ...) = <expr>`
  - 右侧可为 list/array/map/mapEntry 等，具体取决于运行时解构器。
