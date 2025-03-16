# Fluxon AST构建过程

## 1. 抽象语法树概述

抽象语法树(Abstract Syntax Tree, AST)是源代码的树状表示，它捕获了程序的结构和语义，同时忽略了语法细节（如括号、分号等）。在Fluxon编译器中，AST是连接语法分析和语义分析的桥梁，为后续的类型检查、优化和代码生成提供基础。

## 2. AST节点设计

### 2.1 节点层次结构

Fluxon的AST节点采用面向对象的设计，形成一个继承层次结构：

```
Node (基类)
├── Statement
│   ├── ExpressionStmt
│   ├── IfStmt
│   ├── WhenStmt
│   ├── ReturnStmt
│   ├── TryStmt
│   └── BlockStmt
├── Declaration
│   ├── FunctionDecl
│   └── VariableDecl
└── Expression
    ├── BinaryExpr
    ├── UnaryExpr
    ├── CallExpr
    ├── VariableExpr
    ├── LiteralExpr
    ├── ListExpr
    ├── MapExpr
    ├── AwaitExpr
    └── ...
```

### 2.2 节点属性

每种节点类型包含特定的属性，用于表示其语法和语义特性：

- **FunctionDecl**：名称、参数列表、返回类型、函数体
- **VariableDecl**：名称、类型、初始化表达式
- **IfStmt**：条件表达式、then分支、else分支
- **BinaryExpr**：左操作数、操作符、右操作数
- **CallExpr**：被调用对象、参数列表

### 2.3 访问者模式

AST节点实现访问者模式，允许不同的处理器遍历和操作树结构：

```java
interface NodeVisitor<T> {
    T visitBinaryExpr(BinaryExpr expr);
    T visitUnaryExpr(UnaryExpr expr);
    T visitCallExpr(CallExpr expr);
    T visitTryExpr(TryStmt expr);
    // ... 其他访问方法
}

abstract class Node {
    abstract <T> T accept(NodeVisitor<T> visitor);
}

class BinaryExpr extends Expression {
    final Expression left;
    final Token operator;
    final Expression right;
    
    // 构造函数...
    
    @Override
    <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitBinaryExpr(this);
    }
}
```

## 3. AST构建过程

### 3.1 自底向上构建

Fluxon的AST构建采用自底向上的方法，从基本表达式开始，逐步构建更复杂的结构：

1. 解析基本表达式（字面量、标识符等）
2. 组合形成复合表达式（二元表达式、函数调用等）
3. 构建语句和声明
4. 最终形成完整程序的AST

### 3.2 表达式构建

以二元表达式为例，构建过程如下：

```java
// 解析二元表达式
Expression parseBinaryExpr(Expression left, Token operator) {
    // 获取当前操作符的优先级
    int precedence = getPrecedence(operator.type);
    
    // 解析右操作数，传入当前优先级
    Expression right = parseExpression(precedence);
    
    // 构建二元表达式节点
    return new BinaryExpr(left, operator, right);
}
```

### 3.3 函数调用构建

Fluxon的函数调用有两种形式：传统括号调用和无括号调用。两者的AST构建过程不同：

#### 3.3.1 传统括号调用

```java
// 解析传统括号调用
Expression parseParenCall(Expression callee) {
    List<Expression> arguments = new ArrayList<>();
    
    if (!check(TokenType.RIGHT_PAREN)) {
        do {
            arguments.add(parseExpression(0));
        } while (match(TokenType.COMMA));
    }
    
    consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
    
    return new CallExpr(callee, arguments);
}
```

#### 3.3.2 无括号调用

```java
// 解析无括号调用
Expression parseImplicitCall(Expression callee) {
    List<Expression> arguments = new ArrayList<>();
    
    // 收集所有参数，直到遇到语句结束或优先级更高的标记
    while (!isAtEnd() && !isStatementEnd() && getCurrentPrecedence() <= CALL_PRECEDENCE) {
        // 处理隐式字符串转换
        if (match(TokenType.IDENTIFIER) && !isVariableReference()) {
            arguments.add(new StringLiteral(previous().lexeme));
        } else {
            arguments.add(parseExpression(CALL_PRECEDENCE));
        }
    }
    
    return new CallExpr(callee, arguments);
}
```

### 3.4 函数调用链构建

函数调用链的构建是Fluxon AST构建的一个特殊挑战：

```
print add min 5 3 3  // 等价于 print(add(min(5, 3), 3))
```

构建过程利用优先级爬升算法：

1. 解析第一个函数名 `print` 作为调用者
2. 解析第二个函数名 `add` 作为第一个参数
3. 发现 `add` 后面有参数，将 `add` 及其参数构建为一个 `CallExpr`
4. 将这个 `CallExpr` 作为 `print` 的参数

```java
// 函数调用链解析示例（简化版）
Expression parseCallChain() {
    // 解析第一个函数名
    Expression callee = parsePrimary();
    
    // 检查是否有后续标记作为参数
    if (canBeArgument(peek())) {
        List<Expression> arguments = new ArrayList<>();
        
        // 解析第一个参数（可能是另一个函数调用）
        Expression arg = parseExpression(CALL_PRECEDENCE);
        
        // 如果参数本身是一个函数调用链，它会被递归处理
        arguments.add(arg);
        
        // 解析剩余参数
        while (canBeArgument(peek())) {
            arguments.add(parseExpression(CALL_PRECEDENCE));
        }
        
        return new CallExpr(callee, arguments);
    }
    
    return callee;
}
```

## 4. 特殊语法的AST表示

### 4.1 变量引用前缀

Fluxon使用 `&` 前缀引用变量值：

```
val x = 5
print(&x)  // 引用变量x的值
```

在AST中表示为 `VariableExpr` 节点：

```java
class VariableExpr extends Expression {
    final String name;
    final boolean isReference; // 标记是否为变量引用（带&前缀）
    
    VariableExpr(String name, boolean isReference) {
        this.name = name;
        this.isReference = isReference;
    }
    
    // ...
}
```

### 4.2 隐式字符串转换

在函数调用参数位置，未加引号的标识符被自动转换为字符串：

```
equip lather_head  // 等价于 equip("lather_head")
```

在AST中，这些隐式字符串直接表示为 `StringLiteral` 节点：

```java
class StringLiteral extends LiteralExpr {
    final String value;
    
    StringLiteral(String value) {
        this.value = value;
    }
    
    // ...
}
```

### 4.3 If表达式

Fluxon的if语句是表达式，可以返回值：

```
def max(a, b) = if &a > &b then &a else &b
```

在AST中表示为 `IfExpr` 节点：

```java
class IfExpr extends Expression {
    final Expression condition;
    final Expression thenBranch;
    final Expression elseBranch; // 可能为null
    
    // ...
}
```

### 4.4 When表达式

Fluxon的when语句支持模式匹配：

```
when &value {
    1 -> "One"
    in 2..5 -> "Between 2 and 5"
    is String -> "It's a string"
    else -> "Unknown"
}
```

在AST中表示为 `WhenExpr` 节点，包含一系列 `Case` 节点：

```java
class WhenExpr extends Expression {
    final Expression subject; // 可能为null（无主体的when）
    final List<Case> cases;
    
    // ...
}

class Case {
    final Pattern pattern;
    final Expression body;
    
    // ...
}

interface Pattern {
    // 不同类型的模式
}

class LiteralPattern implements Pattern {
    final LiteralExpr value;
    // ...
}

class RangePattern implements Pattern {
    final Expression start;
    final Expression end;
    final boolean inclusive;
    // ...
}

class TypePattern implements Pattern {
    final String typeName;
    // ...
}

class WildcardPattern implements Pattern {
    // 表示 "else" 或 "_" 模式
    // ...
}
```

### 4.5 简单异常处理

Fluxon只支持简单的异常处理，不能捕获特定类型的异常：

```
try {
    riskyOperation()
} catch {
    print("Error occurred")
}
```

在AST中表示为 `TryStmt` 节点：

```java
class TryStmt extends Statement {
    final BlockStmt tryBlock;
    final BlockStmt catchBlock;
    
    // 注意：没有异常类型和异常变量
    
    TryStmt(BlockStmt tryBlock, BlockStmt catchBlock) {
        this.tryBlock = tryBlock;
        this.catchBlock = catchBlock;
    }
    
    // ...
}
```

与Java的try-catch不同，Fluxon的异常处理有以下限制：

1. 不支持捕获特定类型的异常
2. 不能访问异常对象
3. 不支持finally块
4. 所有异常都被统一处理

## 5. AST验证与优化

### 5.1 结构验证

在AST构建完成后，进行结构验证以确保树的完整性和一致性：

- 检查所有必需的子节点是否存在
- 验证节点之间的关系是否合法
- 确保语法结构的完整性

### 5.2 初步优化

在语义分析之前，可以进行一些初步的AST优化：

- 常量折叠：计算编译时可确定的表达式
- 死代码消除：移除永不执行的代码分支
- 表达式简化：简化冗余或复杂的表达式

```java
class ConstantFolder implements NodeVisitor<Expression> {
    @Override
    public Expression visitBinaryExpr(BinaryExpr expr) {
        Expression left = expr.left.accept(this);
        Expression right = expr.right.accept(this);
        
        // 如果两个操作数都是常量，计算结果
        if (left instanceof LiteralExpr && right instanceof LiteralExpr) {
            return evaluateConstantExpression(left, expr.operator, right);
        }
        
        // 否则返回可能优化后的表达式
        if (left != expr.left || right != expr.right) {
            return new BinaryExpr(left, expr.operator, right);
        }
        
        return expr;
    }
    
    // ... 其他访问方法
}
```

## 6. AST序列化与可视化

### 6.1 序列化

为了支持增量编译和调试，Fluxon的AST可以序列化为JSON或其他格式：

```java
class AstSerializer implements NodeVisitor<JsonObject> {
    @Override
    public JsonObject visitBinaryExpr(BinaryExpr expr) {
        JsonObject json = new JsonObject();
        json.add("type", "BinaryExpr");
        json.add("operator", expr.operator.lexeme);
        json.add("left", expr.left.accept(this));
        json.add("right", expr.right.accept(this));
        return json;
    }
    
    // ... 其他访问方法
}
```

### 6.2 可视化

为了调试和教学目的，Fluxon提供AST可视化工具：

- 树形图表示
- 交互式浏览
- 节点属性检查

这些工具帮助开发者理解代码的结构和编译过程。

## 7. AST到IR的转换

### 7.1 类型处理

由于Fluxon不支持泛型，AST到IR的转换过程中对类型的处理相对简单：

1. 所有类型引用都是简单名称
2. Java泛型类型被视为原始类型
3. 不需要处理类型参数和类型约束

```java
// 类型转换示例
class TypeConverter implements NodeVisitor<IRType> {
    @Override
    public IRType visitTypeReference(TypeReference typeRef) {
        // 简单类型转换，不处理泛型
        return new IRSimpleType(typeRef.name);
    }
    
    // ...
}
```

### 7.2 异常处理转换

将Fluxon的简单异常处理转换为IR：

```java
// 异常处理转换
class ExceptionHandler implements NodeVisitor<IRNode> {
    @Override
    public IRNode visitTryStmt(TryStmt stmt) {
        IRBlock tryBlock = (IRBlock)stmt.tryBlock.accept(this);
        IRBlock catchBlock = (IRBlock)stmt.catchBlock.accept(this);
        
        // 创建通用异常处理IR节点
        return new IRTryCatch(tryBlock, catchBlock);
    }
    
    // ...
}
```

### 7.3 完整转换流程

AST构建完成后，将进入语义分析阶段，然后转换为中间表示(IR)：

1. 语义分析器遍历AST，执行类型检查和推断
2. 解析变量引用和作用域
3. 将AST转换为更接近目标代码的IR形式
4. IR进入优化阶段，然后生成最终代码

这个过程确保了从源代码到可执行代码的平滑转换，同时保留了程序的语义。