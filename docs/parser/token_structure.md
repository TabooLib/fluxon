# Fluxon 标记结构设计

## 1. 标记(Token)概述

标记(Token)是语法分析器处理的基本单位，由词法分析器生成。每个标记代表源代码中的一个语法元素，如关键字、标识符、操作符或字面量。Fluxon的标记设计旨在支持语言的特殊语法特性，如灵活的函数调用、变量引用前缀等。

## 2. 标记类型

Fluxon语言的标记分为以下几类：

### 2.1 关键字(Keywords)

关键字是语言预定义的保留字，具有特定的语法含义：

- `def` - 函数定义
- `val` - 不可变变量声明
- `var` - 可变变量声明
- `if` - 条件语句
- `then` - 条件语句的结果部分
- `else` - 条件语句的替代部分
- `when` - 模式匹配语句
- `in` - 范围检查或迭代
- `is` - 类型检查
- `async` - 异步函数定义
- `await` - 等待异步操作
- `try` - 异常处理
- `catch` - 捕获异常
- `return` - 函数返回

### 2.2 标识符(Identifiers)

标识符用于命名变量、函数和其他用户定义的实体：

- 普通标识符：`name`, `factorial`, `loadUser`
- 变量引用：`&x`, `&counter`, `&result`（带有 `&` 前缀）

### 2.3 字面量(Literals)

表示固定值的标记：

- 整数字面量：`123`, `0`, `-42`
- 浮点数字面量：`3.14`, `-0.01`, `1e-10`
- 字符串字面量：`"Hello, World!"`, `"Line 1\nLine 2"`
- 布尔字面量：`true`, `false`
- 空值字面量：`null`

### 2.4 操作符(Operators)

表示操作的符号：

- 算术操作符：`+`, `-`, `*`, `/`, `%`
- 比较操作符：`==`, `!=`, `<`, `>`, `<=`, `>=`
- 逻辑操作符：`&&`, `||`, `!`
- 赋值操作符：`=`
- 范围操作符：`..`, `..<`
- 空安全操作符：`?.`, `?:`
- 其他特殊操作符：`->` (用于when语句)

### 2.5 分隔符(Delimiters)

用于分隔语法结构的符号：

- 括号：`(`, `)`, `{`, `}`, `[`, `]`
- 逗号：`,`
- 冒号：`:`
- 分号：`;`
- 点：`.`

## 3. 标记结构

每个标记包含以下信息：

```
Token {
    type: TokenType,       // 标记类型
    lexeme: String,        // 原始文本
    literal: Object,       // 解析后的值（对于字面量）
    line: int,             // 行号
    column: int,           // 列号
    sourceFile: String     // 源文件名
}
```

### 3.1 TokenType枚举

```
enum TokenType {
    // 关键字
    DEF, VAL, VAR, IF, THEN, ELSE, WHEN, IN, IS, ASYNC, AWAIT, TRY, CATCH, RETURN,
    
    // 标识符
    IDENTIFIER, VARIABLE_REF,
    
    // 字面量
    INTEGER, FLOAT, STRING, BOOLEAN, NULL,
    
    // 操作符
    PLUS, MINUS, STAR, SLASH, PERCENT,
    EQUAL_EQUAL, BANG_EQUAL, LESS, GREATER, LESS_EQUAL, GREATER_EQUAL,
    AND, OR, BANG,
    EQUAL,
    DOT_DOT, DOT_DOT_LESS,
    QUESTION_DOT, QUESTION_COLON,
    ARROW,
    
    // 分隔符
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, LEFT_BRACKET, RIGHT_BRACKET,
    COMMA, COLON, SEMICOLON, DOT,
    
    // 特殊标记
    EOF
}
```

## 4. 特殊标记处理

### 4.1 变量引用前缀

Fluxon使用 `&` 前缀来引用变量值。语法分析器需要特别处理这种情况：

```
val x = 5
print(&x)  // 引用变量x的值
```

在词法分析阶段，`&x` 被识别为 `VARIABLE_REF` 类型的标记，而不是两个独立的标记（`&` 和 `x`）。

### 4.2 隐式字符串

Fluxon允许在参数位置使用未加引号的标识符作为字符串：

```
equip lather_head  // 等价于 equip("lather_head")
```

这种情况下，词法分析器将 `lather_head` 识别为 `IDENTIFIER` 类型的标记，而语法分析器在函数调用参数位置遇到标识符时，会将其解释为字符串字面量。

### 4.3 函数调用链

Fluxon支持无括号函数调用和函数调用链：

```
print add min 5 3 3  // 等价于 print(add(min(5, 3), 3))
```

语法分析器需要特别处理这种情况，通过优先级规则正确解析函数调用链。

## 5. 标记流处理

语法分析器从词法分析器接收标记流，并通过以下方法处理：

- **当前标记**：指向当前正在处理的标记
- **前瞻标记**：用于预测性解析，查看下一个标记而不消耗它
- **消耗标记**：移动到下一个标记
- **匹配标记**：验证当前标记是否为预期类型，然后消耗它
- **同步**：在遇到语法错误时，跳过标记直到找到可以继续解析的点

这种设计使得语法分析器能够高效地处理标记流，并在遇到错误时进行恢复。