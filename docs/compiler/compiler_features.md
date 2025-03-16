# Fluxon 编译器特性设计

## 1. 严格模式(Strict Mode)

### 1.1 概述

Fluxon支持严格模式，通过在文件头声明 `#!strict` 启用。严格模式限制了语言的动态特性，强制执行更严格的类型检查和语法规则，有助于提前发现错误并生成更高效的代码。

### 1.2 语法解析差异

在严格模式下，语法分析器需要处理以下差异：

#### 1.2.1 禁用隐式字符串转换

```
// 非严格模式允许
equip lather_head  // 等价于 equip("lather_head")

// 严格模式要求
equip("lather_head")  // 必须使用引号
```

语法分析器在严格模式下会将未加引号的标识符视为变量或函数引用，而不是自动转换为字符串。

#### 1.2.2 强制类型标注

```
// 非严格模式允许
def add(a, b) = &a + &b

// 严格模式要求
def add(a: Int, b: Int): Int = &a + &b
```

在严格模式下，语法分析器会验证：
- 所有函数参数必须有类型标注
- 函数返回值必须有类型标注
- 变量声明必须有类型标注或初始化表达式

#### 1.2.3 禁止无类型变量

```
// 非严格模式允许
x = 5  // 隐式声明变量

// 严格模式要求
val x: Int = 5  // 必须使用val或var声明，并指定类型或提供初始值
```

### 1.3 解析器实现

严格模式的实现需要在语法分析器中添加额外的验证步骤：

1. 在解析文件开头时检测 `#!strict` 指令
2. 根据模式设置解析器的状态标志
3. 在相关语法规则中添加条件检查
4. 在违反严格模式规则时生成适当的错误消息

```java
// 伪代码示例
class Parser {
    private boolean strictMode = false;
    
    void parseProgram() {
        // 检查是否有严格模式指令
        if (match(TokenType.HASH_BANG) && matchLexeme("strict")) {
            strictMode = true;
            consume(); // 消耗指令
        }
        
        // 继续解析程序
        // ...
    }
    
    Expression parseArgument() {
        if (strictMode && match(TokenType.IDENTIFIER) && !isVariableReference()) {
            // 严格模式下不允许隐式字符串
            error("Implicit strings are not allowed in strict mode. Use quotes.");
            return null;
        }
        
        // 正常解析参数
        // ...
    }
    
    // 其他方法...
}
```

## 2. 选择性静态化(Static Hints)

### 2.1 概述

选择性静态化是Fluxon的一个关键特性，允许开发者通过类型标记或注解，引导编译器生成高效的静态代码。这种机制在保持语言动态特性的同时，为性能关键部分提供静态优化的能力。

### 2.2 类型标注语法

```
// 参数类型标注
def factorial(n: Int): Int = {
  if &n <= 1 then 1
  else &n * factorial(&n - 1)
}

// 变量类型标注
val counter: Int = 0
var name: String = "Default"
```

语法分析器需要解析这些类型标注，并将其包含在AST中，以便后续的类型检查和代码生成阶段使用。

### 2.3 类型推断支持

除了显式类型标注外，Fluxon还支持类型推断：

```
def add(a: Int, b) = &a + &b  // b未标注，但根据 &a + &b 推断为Int
```

语法分析器需要构建足够详细的AST，以便类型推断系统能够分析表达式上下文并确定类型。

### 2.4 解析器实现

选择性静态化的解析实现包括：

1. 解析类型标注语法
2. 在AST节点中保存类型信息
3. 为类型推断提供必要的上下文信息

```java
// 类型标注的AST表示
class TypeAnnotation {
    final String typeName;
    // Fluxon不支持泛型，所以不需要类型参数
    
    // ...
}

// 带类型标注的变量声明
class VariableDecl extends Declaration {
    final String name;
    final TypeAnnotation type; // 可能为null（类型推断）
    final Expression initializer;
    
    // ...
}
```

## 3. 中间表示(IR)设计

### 3.1 概述

Fluxon的中间表示(IR)是连接前端（解析器）和后端（代码生成）的桥梁。IR设计考虑了静态优化和动态特性的平衡，为不同的执行模式提供支持。

### 3.2 IR节点类型

Fluxon的IR包含以下主要节点类型：

- **值节点**：表示常量、变量引用等
- **操作节点**：表示算术、逻辑等操作
- **控制流节点**：表示条件、循环等控制结构
- **函数节点**：表示函数定义和调用
- **类型节点**：表示类型信息和类型操作

### 3.3 从AST到IR的转换

语法分析器生成AST后，需要进行AST到IR的转换：

1. 解析变量引用和作用域
2. 应用类型推断
3. 处理隐式转换
4. 优化常量表达式
5. 转换为IR节点

这个过程在保留程序语义的同时，为后续优化提供更适合的表示形式。

## 4. 编译优化策略

### 4.1 静态类型优先

当检测到静态类型信息时，Fluxon编译器优先生成基于静态类型的代码：

```
// 使用静态类型信息生成高效代码
def sum(numbers: Array): Int = {
    var total: Int = 0
    for n in &numbers {
        total = &total + &n
    }
    &total
}
```

语法分析器需要准确捕获类型信息，以便后续阶段能够应用静态优化。

### 4.2 动态类型降级

对于未提供类型信息的代码，Fluxon采用动态类型处理：

```
// 动态类型处理
def process(data) = {
    // 类型在运行时确定
    &data.transform()
}
```

语法分析器需要构建适当的AST结构，以支持动态类型解析和方法调用。

### 4.3 隐式字符串转换优化

Fluxon的隐式字符串转换在语法分析阶段处理：

```
// 源代码
equip lather_head

// 转换后的AST
CallExpr("equip", [StringLiteral("lather_head")])
```

这种早期转换避免了运行时的字符串解析开销。

### 4.4 函数调用链优化

函数调用链的解析和优化是Fluxon的一个关键特性：

```
// 源代码
print add min 5 3 3

// 优化的调用结构
CallExpr("print", [
    CallExpr("add", [
        CallExpr("min", [IntLiteral(5), IntLiteral(3)]),
        IntLiteral(3)
    ])
])
```

语法分析器通过优先级爬升算法构建这种嵌套结构，为后续优化提供基础。

## 5. 异步处理设计

### 5.1 异步语法支持

Fluxon支持async/await语法进行异步编程：

```
async def fetchData() = {
    await http.get("https://api.com/data")
}
```

语法分析器需要特殊处理这些关键字和结构：

1. 识别async函数声明
2. 解析await表达式
3. 构建表示异步操作的AST节点

### 5.2 异步转换

在AST到IR转换阶段，异步结构被转换为基于CompletableFuture的状态机：

```
// 转换为类似的Java代码
public CompletableFuture<Object> fetchData() {
    return http_get("https://api.com/data").thenCompose(response -> ...);
}
```

这种转换利用了JVM的异步处理能力，同时保持了代码的简洁性。

## 6. 空安全设计

### 6.1 空安全操作符

Fluxon支持空安全操作符，简化空值处理：

```
// 空安全访问
val name = user?.name

// 空合并操作符
val displayName = &name ?: "Guest"
```

语法分析器需要解析这些操作符并构建适当的AST结构，表示条件执行和默认值逻辑。

### 6.2 空安全实现

空安全操作符在编译时转换为条件检查：

```
// 源代码
user?.name

// 转换为等价的条件逻辑
(user != null) ? user.name : null
```

这种转换避免了运行时的空指针异常，同时保持了代码的简洁性。

## 7. 异常处理设计

### 7.1 简单异常处理

Fluxon提供基本的异常处理机制，但不支持高级异常捕获：

```
// Fluxon代码
try {
    riskyOperation()
} catch {
    print("An error occurred")
}

// 转换为Java代码
try {
    riskyOperation();
} catch (Exception e) {
    print("An error occurred");
}
```

Fluxon的异常处理有以下限制：

1. 不支持捕获特定类型的异常
2. 不能访问异常对象的属性
3. 所有异常都被统一处理

### 7.2 编译时异常检查

虽然Fluxon不支持高级异常捕获，但编译器会执行基本的异常安全检查：

1. 检测可能抛出异常但未处理的代码
2. 在严格模式下强制要求处理可能的异常
3. 提供编译时警告，指导开发者添加适当的异常处理

## 8. 类型系统限制

### 8.1 无泛型支持

Fluxon不支持泛型，这对类型系统有以下影响：

1. 所有集合类型都是非泛型的
2. Java泛型类型在Fluxon中被视为原始类型
3. 类型安全性降低，需要更多的运行时类型检查

### 8.2 类型擦除处理

当与Java泛型API交互时，Fluxon采用类型擦除策略：

```
// Java代码
List<String> list = new ArrayList<>();

// Fluxon等效代码
val list = ArrayList()  // 原始类型，没有类型参数
```

编译器会生成必要的类型转换代码，确保与Java泛型API的兼容性。

## 9. 增量编译支持

### 7.1 文件级依赖跟踪

Fluxon编译器支持增量编译，通过跟踪文件间的依赖关系，只重新编译受影响的部分：

1. 解析器记录导入和引用关系
2. 构建依赖图
3. 在文件更改时，只重新编译受影响的文件

### 7.2 AST缓存

为支持增量编译，Fluxon实现了AST缓存机制：

1. 缓存未修改文件的AST
2. 在重新编译时复用缓存的AST
3. 只对修改的部分进行类型检查和代码生成

这种机制显著提高了大型项目的编译速度。

## 10. 调试支持

### 8.1 源码映射

Fluxon编译器生成源码映射信息，将生成的字节码映射回原始源代码：

1. 在AST和IR中保留源码位置信息
2. 在代码生成时包含行号表
3. 支持源码级调试

### 8.2 调试钩子

解析器生成的AST包含调试钩子，支持断点、单步执行等调试功能：

1. 在语句级别插入调试检查点
2. 支持变量检查和修改
3. 提供运行时堆栈跟踪

这些特性使Fluxon成为一个开发者友好的DSL，既有高性能，又有良好的开发体验。