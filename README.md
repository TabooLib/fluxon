# Fluxon

Fluxon 是一种强类型、编译型的 Java 领域特定语言（DSL）。

## 语言特性

### 基础语法

- **变量**：用 `&` 前缀引用变量值，无前缀的标识符默认解析为字符串或函数名。
  ```
  val x = 5       // 定义变量
  print(&x)       // 输出 5
  ```
- **字符串**：未加引号的标识符在参数位置自动转为字符串。
  ```
  equip lather_head  // 等价于 equip("lather_head")
  print factorial 5  // 等价于 print(factorial(5))
  ```
- **返回值**：最后一个表达式为返回值，`return` 可选。
  ```
  def add(a, b) = { &a + &b }  // 隐式返回
  ```

### 函数定义

- **灵活括号**：单行函数可省略 `{}`，支持无括号调用。
  ```
  def greet(name) = "Hello, ${name}!"
  greet "World"    // 输出 "Hello, World!"
  ```
  ```
  print add min 5 3 3 // 等价于 print(add(min(5, 3), 3)) 输出 6
  ```
- **异步支持**：`async/await` 关键字。
  ```
  async def fetchData() = {
    await http.get("https://api.com/data")
  }
  ```

### 控制流

- **`if` 表达式**：单行可省略 `{}`，支持表达式返回值。
  ```
  def max(a, b) = if &a > &b then &a else &b
  ```
- **`when` 语句**：类似 Kotlin，支持模式匹配。
  ```
  when &value {
    1 -> "One"
    in 2..5 -> "Between 2 and 5"
    is String -> "It's a string"
    else -> "Unknown"
  }
  ```

### 集合与字面量

- **列表/字典**：简洁初始化语法。
  ```
  val nums = [1, 2, 3]
  val user = { name: "Bob", age: 25 }
  ```
- **范围表达式**：`1..10` 或 `start..<end`。

### 错误处理

- **`try` 表达式**：直接返回值。
  ```
  val result = try { riskyOp() } catch { "default" }
  ```

### 系统函数与顶层代码

- **顶层执行**：无需 `main` 函数。
  ```
  print factorial 5  // 直接执行
  ```
- **内联系统函数**：如 `sleep 10`。

### 其他高级特性

- **类型推断**：动态类型，支持可选类型注解。
  ```
  def add(a: Int, b: Int): Int = &a + &b
  ```
- **空安全**：`?.` 和 `?:` 操作符。
  ```
  val name = user?.name ?: "Guest"
  ```

### 优先级与结合性规则、

- 函数调用优先级最高：
  ```
  min 1 + 2 3 解析为 min(1 + 2, 3)
  ```
- 算术操作符优先级：`*` `/` > `+` `-` > 比较运算符。

### 完整示例

```
// 递归函数
def factorial(n) = {
    if &n <= 1 then 1
    else &n * factorial(&n - 1)
}

// 异步数据加载
async def loadUser(id) = {
    await fetch("users/${id}")
}

// 使用 when 处理多种情况
def describe(num) = when {
    &num % 2 == 0 -> "even"
    &num < 0 -> "negative odd"
    else -> "positive odd"
}

// 空安全
val names = user?.fullName?.split(" ") ?: ["Unknown"]

// 顶层代码直接执行
print checkGrade 85  // 输出 B
print factorial 5    // 输出 120
```

## 编译优化

### 选择性静态化（Static Hints）

允许开发者通过类型标记或注解，引导编译器生成高效静态代码。

- 使用 `:Type` 注解参数或变量类型。
- 若未标注类型，默认按动态类型解析，但编译器尝试推断。

```
// 显式静态类型（编译为原生 int 操作）
def factorial(n: Int): Int = {
  if &n <= 1 then 1
  else &n * factorial(&n - 1)
}

// 类型推断（编译器推断 a 和 b 为 Int）
def add(a: Int, b) = &a + &b  // b 未标注，但根据 &a + &b 推断为 Int
```

### Strict 模式

在文件头声明 `#!strict`，启用严格模式，限制动态特性。

- **禁用隐式字符串转换**：参数必须明确为字符串或变量。
- **强制类型标注**：函数参数和返回值必须显式声明类型。
- **禁止无类型变量**：所有变量需通过 `val` 或 `var` 声明。

```
#!strict  // 启用 Strict 模式

// 合法代码
def equip(item: String) = {
  println("Equipping: " + item)
}

// 非法代码（Strict 模式下）
def greet(name) = "Hello, ${name}!"  // 错误：未声明 name 类型
equip lather_head                     // 错误：需写成 equip("lather_head")
```

## 编译器

### 编译器架构设计

采用 分层编译架构，分为前端（解析+语义分析）与后端（优化+代码生成）：

```
flowchart LR
    Lexer --> Parser --> AST
    AST --> Semantic[语义分析+类型推断]
    Semantic --> IR[中间表示]
    IR --> Optimizer --> CodeGen[JVM字节码生成]
```

### 关键优化策略

#### 选择性静态化

+ 静态类型优先：通过类型注解和类型推断，优先生成原生类型操作（如 `int` 直接运算而非 `Integer` 对象）。
+ 动态类型降级：未标注类型时，使用 `invokedynamic` 指令动态绑定方法，但通过类型特化缓存提升性能。

示例：`factorial(n:Int)` 函数编译为：

```java
// 静态方法直接使用int类型
public static int factorial(int n) {
    return (n <= 1) ? 1 : n * factorial(n - 1);
}
```

#### 隐式字符串转换优化

在语法分析阶段，将无引号参数自动包装为字符串字面量：

```
// Fluxon 代码: equip lather_head
// 转换为 AST 节点: CallExpr("equip", StringLiteral("lather_head"))
```

#### 函数调用链解析

使用 优先级爬升算法（Pratt Parsing） 处理函数调用优先级：

```
// Fluxon 代码: print add min 5 3 3
// 解析为: print(add(min(5,3), 3))
```

#### 异步处理优化

将 `async/await` 编译为 基于 `CompletableFuture` 的状态机，利用 JVM 的轻量级线程（Loom）减少上下文切换开销：

```
// Fluxon 异步函数
async def fetchData() = { await http.get(url) }

// 转换为 Java 代码
public CompletableFuture<Object> fetchData() {
    return http_get(url).thenCompose(response -> ...);
}
```

#### 类型推断与特化

+ 局部类型推断：基于变量使用上下文推断类型（如 `&a + &b` 推断 `a` 和 `b` 为 `int`）。
+ 多版本方法生成：为同一函数生成静态和动态版本，运行时根据参数类型选择最优版本。

#### 空安全

空安全操作符 `?.` 编译为条件跳转，避免反射调用：

```
// Fluxon 代码: user?.name
// 转换为 Java: (user != null) ? user.name : null
```

#### 集合与字面量优化

+ 不可变集合优化：`[1,2,3]` 直接编译为 `List.of(1,2,3)`，减少内存分配。
+ 字典字面量使用 `Map.ofEntries` 生成高效实例。

#### 严格模式实现

在严格模式下，启用 **编译时强类型检查**，禁止动态特性：

```
// Strict 模式下的类型错误
def greet(name) = "Hello, ${name}!"  // 编译错误: 参数 name 未声明类型
```

#### 运行时性能对比

| 特性    | Fluxon (静态模式) | Nashorn | JEXL    |
|-------|---------------|---------|---------|
| 数值计算  | 原生 int 操作     | 动态对象    | 反射      |
| 函数调用  | 直接方法调用        | 动态绑定    | 解释执行    |
| 内存占用  | 低（无装箱）        | 高（对象）   | 中等（对象）  |
| 异步吞吐量 | 高（Loom支持）     | 低（同步）   | 低（解释执行） |