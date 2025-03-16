# Fluxon 语法规则定义

## 1. 语法规则概述

Fluxon语言的语法规则定义了源代码的结构和组织方式。这些规则采用扩展的BNF(巴科斯-诺尔范式)表示，清晰地描述了语言的各个构造。本文档详细说明Fluxon语言的语法规则，为语法分析器的实现提供基础。

## 2. 程序结构

### 2.1 程序

一个Fluxon程序由一系列声明和语句组成：

```
program        → declaration* EOF
declaration    → functionDecl | variableDecl | statement
```

### 2.2 声明

#### 2.2.1 函数声明

```
functionDecl   → "def" IDENTIFIER "(" parameters? ")" [":" simpleName] "=" functionBody
parameters     → parameter ("," parameter)*
parameter      → IDENTIFIER [":" simpleName]
functionBody   → expression | "{" statement* "}"
```

异步函数声明：

```
asyncFunctionDecl → "async" "def" IDENTIFIER "(" parameters? ")" [":" simpleName] "=" functionBody
```

#### 2.2.2 变量声明

```
variableDecl  → ("val" | "var") IDENTIFIER [":" simpleName] "=" expression
```

## 3. 语句

```
statement      → exprStmt | ifStmt | whenStmt | returnStmt | tryStmt | block
exprStmt       → expression
block          → "{" statement* "}"
```

### 3.1 条件语句

```
ifStmt         → "if" expression "then" statement ("else" statement)?
```

### 3.2 When语句

```
whenStmt       → "when" (expression)? "{" whenCase* "}"
whenCase       → pattern "->" statement
pattern        → literal
               | IDENTIFIER
               | "in" rangeExpr
               | "is" simpleName
               | "_"
```

### 3.3 返回语句

```
returnStmt     → "return" expression?
```

### 3.4 异常处理语句

```
tryStmt        → "try" block "catch" block
```

注意：Fluxon只支持简单的异常处理，不支持捕获特定类型的异常或访问异常对象。

## 4. 表达式

```
expression     → assignment
assignment     → IDENTIFIER "=" expression
               | logicOr
```

### 4.1 逻辑表达式

```
logicOr        → logicAnd ("||" logicAnd)*
logicAnd       → equality ("&&" equality)*
equality       → comparison (("==" | "!=") comparison)*
comparison     → range (("<" | ">" | "<=" | ">=") range)*
```

### 4.2 范围表达式

```
range          → term ((".." | "..<") term)*
term           → factor (("+" | "-") factor)*
factor         → unary (("*" | "/" | "%") unary)*
```

### 4.3 一元表达式

```
unary          → ("!" | "-") unary | call
```

### 4.4 函数调用

Fluxon的函数调用语法是其最独特的特性之一，支持无括号调用和函数调用链：

```
call           → primary callTail*
callTail       → "(" arguments? ")"
               | expression+
arguments      → expression ("," expression)*
```

这种语法允许以下调用形式：

```
print(x)           // 传统括号调用
print x            // 无括号调用，单参数
print x y z        // 无括号调用，多参数
print add x y      // 函数调用链，等价于 print(add(x, y))
```

### 4.5 基本表达式

```
primary        → IDENTIFIER | VARIABLE_REF | literal | "(" expression ")"
               | listLiteral | mapLiteral | awaitExpr
VARIABLE_REF   → "&" IDENTIFIER
```

### 4.6 字面量

```
literal        → INTEGER | FLOAT | STRING | BOOLEAN | NULL
listLiteral    → "[" (expression ("," expression)*)? "]"
mapLiteral     → "{" (mapEntry ("," mapEntry)*)? "}"
mapEntry       → (IDENTIFIER | STRING) ":" expression
```

### 4.7 异步表达式

```
awaitExpr      → "await" expression
```

### 4.8 空安全操作

```
safeAccess     → primary ("?." IDENTIFIER)*
nullCoalesce   → expression ("?:" expression)*
```

## 5. 类型

```
simpleName     → IDENTIFIER
```

注意：Fluxon不支持泛型，所有类型引用都是简单名称。

## 6. 优先级和结合性

Fluxon语言的操作符优先级从高到低排列如下：

1. 函数调用、成员访问 (`.`, `?.`)
2. 一元操作符 (`!`, `-`)
3. 乘法操作符 (`*`, `/`, `%`)
4. 加法操作符 (`+`, `-`)
5. 范围操作符 (`..`, `..<`)
6. 比较操作符 (`<`, `>`, `<=`, `>=`)
7. 相等操作符 (`==`, `!=`)
8. 逻辑与 (`&&`)
9. 逻辑或 (`||`)
10. 空合并操作符 (`?:`)
11. 赋值操作符 (`=`)

函数调用链的解析采用特殊的优先级规则，确保正确的嵌套关系：

```
print add min 5 3 3  // 解析为 print(add(min(5, 3), 3))
```

## 7. 特殊语法规则

### 7.1 隐式字符串转换

在函数调用参数位置，未加引号的标识符被自动转换为字符串：

```
equip lather_head  // 等价于 equip("lather_head")
```

这在语法规则中表示为：

```
implicitString → IDENTIFIER  // 在函数调用参数位置
```

### 7.2 隐式返回

函数体中的最后一个表达式自动成为返回值，无需显式的return语句：

```
def add(a, b) = { &a + &b }  // 隐式返回 &a + &b
```

### 7.3 可选的函数体大括号

单行函数定义可以省略大括号：

```
def greet(name) = "Hello, ${name}!"  // 无需大括号
```

### 7.4 简单异常处理

Fluxon只支持简单的异常处理，不能捕获特定类型的异常：

```
// Fluxon 代码
try {
    riskyOperation()
} catch {
    print("Error occurred")
}

// 编译为 Java 代码
try {
    riskyOperation();
} catch (Exception e) {
    print("Error occurred");
}
```

## 8. 严格模式语法

在严格模式下（文件头声明 `#!strict`），语法规则有以下变化：

1. 禁用隐式字符串转换
2. 函数参数和返回值必须显式声明类型
3. 所有变量必须通过 `val` 或 `var` 声明

```
// 严格模式下的函数声明
functionDecl   → "def" IDENTIFIER "(" typedParameters ")" ":" simpleName "=" functionBody
typedParameters → typedParameter ("," typedParameter)*
typedParameter  → IDENTIFIER ":" simpleName
```

这些语法规则为Fluxon语法分析器的实现提供了明确的指导，确保语言的一致性和可预测性。