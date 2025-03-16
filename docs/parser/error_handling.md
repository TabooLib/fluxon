# Fluxon 错误处理系统设计

## 1. 概述

错误处理系统是Fluxon编译器的关键组件，负责检测、报告和恢复各种编译错误。一个良好设计的错误处理系统不仅能提供清晰的错误信息，还能在遇到错误时保持编译过程的稳定性，并尽可能多地检测出错误。本文档详细描述了Fluxon错误处理系统的设计。

## 2. 错误报告机制

### 2.1 错误类型分类

Fluxon的错误分为以下几类：

1. **词法错误**：无效的字符序列、未闭合的字符串等
2. **语法错误**：不符合语法规则的结构
3. **类型错误**：类型不匹配、未知类型等
4. **名称错误**：未定义的变量、函数等
5. **导入错误**：无法解析的预导入类型
6. **语义错误**：语义上不合理的代码结构

### 2.2 错误信息结构

```java
class ErrorInfo {
    ErrorType type;           // 错误类型
    String message;           // 错误消息
    SourceLocation location;  // 错误位置
    String sourceFile;        // 源文件
    String sourceLine;        // 错误所在行的代码
    List<String> hints;       // 修复提示
    
    // 格式化错误信息
    String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(sourceFile).append(":").append(location.line).append(":").append(location.column);
        sb.append(": ").append(type).append(": ").append(message).append("\n");
        sb.append("  ").append(sourceLine).append("\n");
        sb.append("  ").append(" ".repeat(location.column - 1)).append("^");
        
        if (!hints.isEmpty()) {
            sb.append("\n提示: ");
            for (String hint : hints) {
                sb.append("\n  - ").append(hint);
            }
        }
        
        return sb.toString();
    }
}
```

### 2.3 错误收集器

```java
class ErrorCollector {
    List<ErrorInfo> errors;
    List<ErrorInfo> warnings;
    boolean hasErrors;
    
    // 报告错误
    void reportError(ErrorType type, String message, SourceLocation location) {
        ErrorInfo error = createErrorInfo(type, message, location);
        errors.add(error);
        hasErrors = true;
    }
    
    // 报告警告
    void reportWarning(String message, SourceLocation location) {
        ErrorInfo warning = createErrorInfo(ErrorType.WARNING, message, location);
        warnings.add(warning);
    }
    
    // 创建错误信息
    private ErrorInfo createErrorInfo(ErrorType type, String message, SourceLocation location) {
        // ...
    }
    
    // 获取所有错误和警告
    List<ErrorInfo> getAllDiagnostics() {
        List<ErrorInfo> result = new ArrayList<>(errors);
        result.addAll(warnings);
        return result;
    }
}
```

## 3. 源码位置跟踪

### 3.1 位置信息结构

```java
class SourceLocation {
    int line;       // 行号（从1开始）
    int column;     // 列号（从1开始）
    int offset;     // 字符偏移量（从0开始）
    int length;     // 跨度长度
    
    // 创建新位置
    SourceLocation(int line, int column, int offset, int length) {
        this.line = line;
        this.column = column;
        this.offset = offset;
        this.length = length;
    }
    
    // 合并两个位置
    SourceLocation merge(SourceLocation other) {
        int startOffset = Math.min(this.offset, other.offset);
        int endOffset = Math.max(this.offset + this.length, other.offset + other.length);
        return new SourceLocation(
            this.line, // 简化，实际实现需要更复杂的逻辑
            this.column,
            startOffset,
            endOffset - startOffset
        );
    }
}
```

### 3.2 源码映射

```java
class SourceMap {
    String sourceCode;
    List<String> lines;
    
    SourceMap(String sourceCode) {
        this.sourceCode = sourceCode;
        this.lines = Arrays.asList(sourceCode.split("\n", -1));
    }
    
    // 获取指定位置的源代码行
    String getLine(int lineNumber) {
        if (lineNumber < 1 || lineNumber > lines.size()) {
            return "";
        }
        return lines.get(lineNumber - 1);
    }
    
    // 获取指定范围的源代码
    String getSourceSnippet(SourceLocation location, int contextLines) {
        // ...
    }
    
    // 从偏移量计算行列号
    SourceLocation getLocationFromOffset(int offset) {
        // ...
    }
}
```

### 3.3 位置传播

在编译过程中，源码位置信息需要从标记传播到AST节点，再传播到IR节点：

```java
// 在AST节点中保存位置信息
abstract class AstNode {
    SourceLocation location;
    
    void setLocation(SourceLocation location) {
        this.location = location;
    }
    
    SourceLocation getLocation() {
        return location;
    }
}

// 从子节点继承位置信息
void propagateLocation(AstNode parent, List<AstNode> children) {
    if (children.isEmpty()) return;
    
    SourceLocation merged = children.get(0).getLocation();
    for (int i = 1; i < children.size(); i++) {
        merged = merged.merge(children.get(i).getLocation());
    }
    
    parent.setLocation(merged);
}
```

## 4. 错误消息格式化

### 4.1 消息模板

为了保持错误消息的一致性，使用预定义的消息模板：

```java
class ErrorTemplates {
    static final String UNDEFINED_VARIABLE = "未定义的变量: '%s'";
    static final String TYPE_MISMATCH = "类型不匹配: 期望 '%s'，实际为 '%s'";
    static final String UNDEFINED_METHOD = "类型 '%s' 没有名为 '%s' 的方法";
    // ...
    
    static String format(String template, Object... args) {
        return String.format(template, args);
    }
}
```

### 4.2 彩色输出

在支持的终端中使用ANSI转义序列实现彩色输出：

```java
class ColoredOutput {
    static final String RESET = "\u001B[0m";
    static final String RED = "\u001B[31m";
    static final String YELLOW = "\u001B[33m";
    static final String BLUE = "\u001B[34m";
    
    static String error(String text) {
        return RED + text + RESET;
    }
    
    static String warning(String text) {
        return YELLOW + text + RESET;
    }
    
    static String highlight(String text) {
        return BLUE + text + RESET;
    }
}
```

### 4.3 错误上下文

为了帮助用户理解错误，提供错误发生的上下文信息：

```java
class ErrorContext {
    // 获取错误上下文
    static String getContext(String sourceCode, SourceLocation location, int contextLines) {
        // 提取错误位置前后的代码行
        // ...
    }
    
    // 生成指向错误位置的箭头
    static String generatePointer(int column, int length) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ".repeat(column - 1));
        sb.append("^");
        if (length > 1) {
            sb.append("~".repeat(length - 1));
        }
        return sb.toString();
    }
}
```

## 5. 错误恢复策略

### 5.1 词法分析器错误恢复

词法分析器在遇到无效字符时的恢复策略：

1. 报告错误
2. 跳过无效字符
3. 尝试继续词法分析

```java
void recoverFromLexicalError() {
    reportError(ErrorType.LEXICAL, "无效字符: '" + currentChar + "'", getCurrentLocation());
    advance(); // 跳过无效字符
}
```

### 5.2 语法分析器错误恢复

语法分析器使用"恐慌模式"进行错误恢复：

1. 报告错误
2. 丢弃标记直到遇到同步点
3. 重置状态并继续解析

```java
void synchronize() {
    advance(); // 跳过导致错误的标记
    
    while (!isAtEnd()) {
        if (previous().type == TokenType.SEMICOLON) return;
        
        switch (peek().type) {
            case DEF:
            case VAL:
            case VAR:
            case IF:
            case WHEN:
            case RETURN:
                return;
        }
        
        advance();
    }
}
```

### 5.3 语义分析器错误恢复

语义分析器在遇到类型错误等问题时：

1. 报告错误
2. 使用推断类型或默认类型继续分析
3. 标记受影响的节点，避免连锁错误

```java
Type checkExpression(Expression expr, Type expectedType) {
    Type actualType = expr.accept(typeChecker);
    
    if (!typeCompatible(actualType, expectedType)) {
        reportError(ErrorType.TYPE, 
            String.format("类型不匹配: 期望 '%s'，实际为 '%s'", expectedType, actualType),
            expr.getLocation());
        
        // 使用期望类型继续分析，避免连锁错误
        return expectedType;
    }
    
    return actualType;
}
```

## 6. 错误修复建议

### 6.1 拼写错误修正

对于未定义的标识符，尝试找到相似的名称作为建议：

```java
List<String> findSimilarNames(String name, Set<String> availableNames) {
    List<String> suggestions = new ArrayList<>();
    
    for (String available : availableNames) {
        if (levenshteinDistance(name, available) <= 2) {
            suggestions.add(available);
        }
    }
    
    return suggestions;
}
```

### 6.2 常见错误模式识别

识别常见的错误模式并提供针对性的修复建议：

```java
void addFixSuggestions(ErrorInfo error) {
    switch (error.type) {
        case UNDEFINED_VARIABLE:
            addSimilarNameSuggestions(error);
            break;
        case MISSING_SEMICOLON:
            error.hints.add("在行尾添加分号");
            break;
        case UNBALANCED_BRACKETS:
            error.hints.add("检查括号是否匹配");
            break;
        // ...
    }
}
```

### 6.3 上下文相关建议

基于错误上下文提供更精确的修复建议：

```java
void addContextualSuggestions(ErrorInfo error, AstNode node) {
    if (error.type == ErrorType.TYPE_MISMATCH && node instanceof MethodCallExpr) {
        MethodCallExpr call = (MethodCallExpr) node;
        List<MethodSignature> alternatives = findCompatibleMethods(call);
        
        for (MethodSignature alt : alternatives) {
            error.hints.add("考虑使用方法: " + alt.toString());
        }
    }
}
```

## 7. 实现计划

### 7.1 第一阶段：基础结构

1. 实现错误信息结构
2. 实现源码位置跟踪
3. 实现基本的错误报告机制

### 7.2 第二阶段：错误恢复

1. 实现词法分析器错误恢复
2. 实现语法分析器错误恢复
3. 实现语义分析器错误恢复

### 7.3 第三阶段：错误增强

1. 实现彩色输出
2. 实现错误修复建议
3. 实现上下文相关建议

### 7.4 第四阶段：集成与测试

1. 与编译器各组件集成
2. 编写错误处理单元测试
3. 进行用户体验测试和改进

## 8. 关键挑战与解决方案

### 8.1 错误级联

**挑战**：一个错误可能导致多个后续错误报告，混淆真正的问题

**解决方案**：
- 实现错误抑制机制，避免报告可能由同一原因引起的多个错误
- 在恢复后标记"可疑区域"，降低该区域内错误的严重性
- 限制每个文件或区域报告的错误数量

### 8.2 错误恢复质量

**挑战**：错误恢复可能导致误导性的后续错误

**解决方案**：
- 使用保守的恢复策略，优先考虑准确性而非完整性
- 在恢复点明确标记，区分原始错误和可能的次生错误
- 提供配置选项，允许用户调整错误恢复的激进程度

### 8.3 错误消息可读性

**挑战**：技术性错误消息对非专业用户不友好

**解决方案**：
- 使用清晰、简洁的语言描述错误
- 提供多级详细程度的错误信息
- 包含具体的代码示例说明如何修复错误