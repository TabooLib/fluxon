# Fluxon 运行时字节码注入

本文档描述 `fs:jvm` 模块的字节码注入功能。

## 概述

`fs:jvm` 模块提供运行时动态注入 Java 方法的能力，可在方法执行前插入逻辑或完全替换方法实现。

**前置要求**：
- 需要 Java Agent 支持（`-javaagent` 启动参数）
- 目标 JVM 版本需支持 `Instrumentation` API

## 模块导入

```fluxon
import 'fs:jvm'
```

导入后可通过 `jvm()` 或 `jvm` 访问模块功能。

## API 概览

| 函数 | 签名 | 说明 |
|------|------|------|
| `inject` | `(target, type, handler) -> String` | 注入方法拦截器 |
| `restore` | `(idOrTarget) -> Boolean` | 撤销注入 |
| `injections` | `() -> List<Map>` | 列出所有活跃注入 |

## inject - 方法注入

### 语法形式

```fluxon
jvm::inject(<target>, <type>, <handler>)
```

### 参数说明

**target**（字符串）- 目标方法标识，支持两种格式：

- 简单格式：`"类全名::方法名"`
  - 示例：`"com.example.Foo::bar"`
  - 匹配该类的所有同名方法重载
  
- 精确格式：`"类全名::方法名(描述符)"`
  - 示例：`"com.example.Foo::bar(Ljava/lang/String;)V"`
  - 仅匹配指定签名的方法
  - 描述符使用 JVM 内部格式（类型签名）

**type**（字符串）- 注入类型：

- `"before"`：在原方法执行前调用 handler
  - handler 返回特殊值可控制流程（见下文）
  
- `"replace"`：完全替换原方法实现
  - handler 的返回值作为方法返回值

**handler**（Lambda）- 回调函数：

- 参数：`|self, arg1, arg2, ...| { ... }`
  - 实例方法：`self` 为 `this`，其余为方法参数
  - 静态方法：所有参数为方法参数
  
- 返回值（`before` 模式）：
  - 返回 `null` 或任意普通值：继续执行原方法
  - 返回 `CallbackDispatcher.SKIP`：跳过原方法执行
  
- 返回值（`replace` 模式）：
  - 返回值将作为原方法的返回值
  - void 方法应返回 `null`

### 返回值

返回注入 ID（字符串），格式为 `inj_<序号>`，用于后续撤销注入。

### 示例

**基础注入（before）**：

```fluxon
id = jvm::inject(
    "com.example.Service::login",
    "before",
    |self, username, password| {
        println("用户尝试登录: " + &username)
        // 返回 null，继续执行原方法
    }
)
```

**方法替换（replace）**：

```fluxon
jvm::inject(
    "com.example.Config::getFeatureFlag",
    "replace",
    |self, flagName| {
        // 强制所有特性开关返回 true
        return true
    }
)
```

**精确匹配方法签名**：

```fluxon
jvm::inject(
    "com.example.Api::call(Ljava/lang/String;I)Ljava/lang/Object;",
    "replace",
    |self, url, timeout| {
        println("拦截 API 调用: " + &url)
        return null
    }
)
```

**条件跳过原方法**：

```fluxon
import 'fs:jvm'

// 注意：需要通过反射或其他方式访问 CallbackDispatcher.SKIP
// 示例简化为返回特定值来跳过
jvm::inject(
    "com.example.Guard::checkPermission",
    "before",
    |self, userId| {
        if &userId == "admin" {
            return true  // 跳过检查
        }
        // 返回 null，执行原检查逻辑
    }
)
```

## restore - 撤销注入

### 语法形式

```fluxon
jvm::restore(<idOrTarget>) -> Boolean
```

### 参数说明

**idOrTarget**（字符串）：

- 注入 ID：撤销特定注入（`inject` 返回的 ID）
- 目标字符串：撤销所有匹配的注入（格式同 `inject` 的 target）

### 返回值

- `true`：成功撤销至少一个注入
- `false`：未找到匹配的注入

### 示例

**按 ID 撤销**：

```fluxon
id = jvm::inject("com.example.Foo::bar", "before", |self| {})
jvm::restore(&id)  // 撤销特定注入
```

**按目标撤销**：

```fluxon
jvm::inject("com.example.Foo::bar", "before", |self| {})
jvm::inject("com.example.Foo::bar", "replace", |self| { return null })

jvm::restore("com.example.Foo::bar")  // 撤销该方法的所有注入
```

## injections - 列出注入

### 语法形式

```fluxon
jvm::injections() -> List<Map>
```

### 返回值

返回所有活跃注入的列表，每项为包含以下字段的 Map：

- `id`（String）：注入 ID
- `target`（String）：目标方法标识
- `type`（String）：注入类型（"before" 或 "replace"）

### 示例

```fluxon
import 'fs:jvm'

jvm::inject("com.example.Foo::bar", "before", |self| {})
jvm::inject("com.example.Baz::qux", "replace", |self| { return 42 })

list = jvm::injections()
// 返回: [
//   [id: "inj_1", target: "com.example.Foo::bar", type: "before"],
//   [id: "inj_2", target: "com.example.Baz::qux", type: "replace"]
// ]

for item in &list {
    println(&item.id + " -> " + &item.target)
}
```

## 类型签名速查

常见 Java 类型的 JVM 描述符表示：

| Java 类型 | JVM 描述符 |
|-----------|-----------|
| `void` | `V` |
| `int` | `I` |
| `long` | `J` |
| `boolean` | `Z` |
| `String` | `Ljava/lang/String;` |
| `Object` | `Ljava/lang/Object;` |
| `int[]` | `[I` |
| `String[]` | `[Ljava/lang/String;` |

**方法签名示例**：
- `void foo()` → `()V`
- `int add(int a, int b)` → `(II)I`
- `String getName()` → `()Ljava/lang/String;`
- `void setName(String name)` → `(Ljava/lang/String;)V`

可使用 `javap -s ClassName` 查看类的完整签名。

## 注意事项

1. **线程安全**：注入注册表是线程安全的，可在多线程环境下调用。

2. **生命周期**：
   - 注入在进程生命周期内有效，除非显式调用 `restore`
   - 已加载的类会立即触发字节码重写（retransform）
   - 未加载的类将在首次加载时应用注入

3. **性能影响**：
   - 注入会引入额外的方法调用开销
   - `before` 模式开销通常小于 `replace` 模式
   - 避免在高频方法上注入复杂逻辑

4. **限制**：
   - 无法注入原生方法（native methods）
   - 无法注入 JVM 内部类（如 `java.lang.*` 部分类）
   - 同一方法可以有多个注入，按注册顺序执行

5. **错误处理**：
   - handler 抛出异常会导致原方法抛出 `RuntimeException`
   - 目标格式错误会在 `inject` 调用时抛出异常
   - Agent 未加载时注入操作会失败并记录警告

6. **调试建议**：
   - 使用 `injections()` 验证注入状态
   - 在 handler 中添加日志输出
   - 测试时优先使用 `before` 模式验证参数捕获
