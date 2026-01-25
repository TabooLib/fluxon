package org.tabooproject.fluxon.compiler;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Type;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 变量类型分析器
 * 在编译期通过两遍扫描确定每个局部变量的统一类型
 *
 * @author sky
 */
public class TypeAnalyzer {

    // 变量位置 -> 统一类型
    private final Map<Integer, Type> variableTypes = new HashMap<>();
    // 上下文调用的 target 类型栈
    private final Deque<Type> targetTypeStack = new ArrayDeque<>();

    /**
     * 从函数定义初始化参数类型
     */
    public void initFromParameterTypes(Map<Integer, Class<?>> parameterTypes) {
        for (Map.Entry<Integer, Class<?>> entry : parameterTypes.entrySet()) {
            variableTypes.put(entry.getKey(), Type.fromClass(entry.getValue()));
        }
    }

    /**
     * 分析 AST，收集变量类型信息
     */
    public void analyze(List<ParseResult> results) {
        for (ParseResult result : results) {
            analyzeNode(result);
        }
    }

    /**
     * 递归分析节点，委托给各自的 Evaluator
     */
    public void analyzeNode(ParseResult node) {
        if (node == null) return;
        if (node instanceof Expression) {
            Expression expr = (Expression) node;
            expr.getExpressionType().evaluator.analyzeTypes(node, this);
        } else if (node instanceof Statement) {
            Statement stmt = (Statement) node;
            stmt.getStatementType().evaluator.analyzeTypes(node, this);
        }
    }

    /**
     * 推断表达式类型，委托给对应的 Evaluator
     */
    public Type inferType(ParseResult expr) {
        if (expr == null) return Type.OBJECT;
        if (expr instanceof Expression) {
            return ((Expression) expr).getExpressionType().evaluator.inferResultType(expr, this);
        }
        return Type.OBJECT;
    }

    /**
     * 记录变量类型，处理类型合并
     */
    public void recordType(int position, Type valueType) {
        Type existing = variableTypes.get(position);
        if (existing == null) {
            variableTypes.put(position, valueType);
        } else if (!existing.equals(valueType)) {
            variableTypes.put(position, Type.OBJECT);
        }
    }

    /**
     * 推断二元运算结果类型
     */
    public Type inferBinaryResultType(Type left, Type right, TokenType op) {
        if (isComparisonOp(op)) return Type.Z;
        if (!left.isPrimitive() || !right.isPrimitive()) return Type.OBJECT;
        if (left == Type.D || right == Type.D) return Type.D;
        if (left == Type.F || right == Type.F) return Type.F;
        if (left == Type.J || right == Type.J) return Type.J;
        if (left == Type.I && right == Type.I) return Type.I;
        return Type.OBJECT;
    }

    /**
     * 强制标记变量为 OBJECT 类型（用于闭包捕获的变量）
     * 被闭包捕获的变量必须使用引用类型，因为 getLocalRef/setLocalRef 支持
     * 环境链穿透，而基本类型存取方法不支持。
     */
    public void markCaptured(int position) {
        variableTypes.put(position, Type.OBJECT);
    }

    /**
     * 获取变量类型
     */
    public Type getVariableType(int position) {
        return variableTypes.getOrDefault(position, Type.OBJECT);
    }

    /**
     * 获取所有变量类型映射
     */
    public Map<Integer, Type> getVariableTypes() {
        return variableTypes;
    }

    /**
     * 压入当前上下文调用的 target 类型
     */
    public void pushTargetType(Type type) {
        targetTypeStack.push(type);
    }

    /**
     * 弹出上下文调用的 target 类型
     */
    public void popTargetType() {
        if (!targetTypeStack.isEmpty()) {
            targetTypeStack.pop();
        }
    }

    /**
     * 获取当前上下文调用的 target 类型
     */
    public Type getCurrentTargetType() {
        return targetTypeStack.isEmpty() ? null : targetTypeStack.peek();
    }

    private boolean isComparisonOp(TokenType op) {
        switch (op) {
            case EQUAL:
            case NOT_EQUAL:
            case GREATER:
            case GREATER_EQUAL:
            case LESS:
            case LESS_EQUAL:
            case IDENTICAL:
            case NOT_IDENTICAL:
                return true;
            default:
                return false;
        }
    }
}
