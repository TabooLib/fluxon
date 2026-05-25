package org.tabooproject.fluxon.compiler;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.parser.expression.literal.IntLiteral;
import org.tabooproject.fluxon.parser.expression.literal.LongLiteral;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Type;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    // root 变量类型
    private Map<String, Type> rootVariableTypes;
    // 脚本内赋值推断出的 root 变量类型，不能覆盖外部显式声明
    private final Map<String, Type> inferredRootVariableTypes = new HashMap<>();
    // 局部变量常量值，仅用于编译期收敛确定安全的循环形态
    private final Map<Integer, Object> localConstants = new HashMap<>();
    // root 变量常量值，仅记录当前编译单元内的直接赋值
    private final Map<String, Object> rootConstants = new HashMap<>();
    // 用户函数定义表，用于在调用点推断返回值类型
    private final Map<String, FunctionDefinition> functionDefinitions = new HashMap<>();
    // 正在推断返回值的函数，避免递归函数导致类型分析无限递归
    private final Set<String> resolvingFunctionReturns = new HashSet<>();

    /**
     * 从函数定义初始化参数类型
     */
    public void initFromParameterTypes(Map<Integer, Class<?>> parameterTypes) {
        for (Map.Entry<Integer, Class<?>> entry : parameterTypes.entrySet()) {
            variableTypes.put(entry.getKey(), Type.fromClass(entry.getValue()));
        }
    }

    /**
     * 设置 root 变量类型
     */
    public void setRootVariableTypes(Map<String, Type> types) {
        this.rootVariableTypes = types;
    }

    /**
     * 设置参数类型
     *
     * @param parameters 参数映射（name -> ParameterInfo）
     */
    public void setParameterTypes(Map<String, ParameterInfo> parameters) {
        if (parameters == null || parameters.isEmpty()) return;
        for (ParameterInfo info : parameters.values()) {
            variableTypes.put(info.getIndex(), info.getType());
        }
    }

    /**
     * 获取 root 变量类型
     *
     * @param name 变量名
     * @return 类型，默认返回 OBJECT
     */
    public Type getRootVariableType(String name) {
        if (rootVariableTypes != null) {
            Type type = rootVariableTypes.get(name);
            if (type != null) return type;
        }
        Type inferredType = inferredRootVariableTypes.get(name);
        if (inferredType != null) return inferredType;
        return Type.OBJECT;
    }

    /**
     * 分析 AST，收集变量类型信息
     */
    public void analyze(List<ParseResult> results) {
        for (ParseResult result : results) {
            if (result instanceof FunctionDefinition) {
                registerFunctionDefinition((FunctionDefinition) result);
            }
        }
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
        } else if (node instanceof FunctionDefinition) {
            // 分析用户定义函数体（需要临时初始化参数类型）
            FunctionDefinition funcDef = (FunctionDefinition) node;
            registerFunctionDefinition(funcDef);
            Map<Integer, Type> savedTypes = new HashMap<>(variableTypes);
            // 初始化参数类型
            for (Map.Entry<Integer, Class<?>> entry : funcDef.getParameterTypes().entrySet()) {
                variableTypes.put(entry.getKey(), Type.fromClass(entry.getValue()));
            }
            // 分析函数体
            analyzeNode(funcDef.getBody());
            // 恢复变量类型（函数作用域隔离）
            variableTypes.clear();
            variableTypes.putAll(savedTypes);
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
     * 推断脚本内用户函数返回类型
     * 用户函数调用参与变量类型合并，否则 root 变量会因调用表达式退化为 OBJECT。
     */
    public Type inferUserFunctionReturnType(FunctionCallExpression expr, Type[] argumentTypes) {
        FunctionDefinition definition = functionDefinitions.get(expr.getFunctionName());
        if (definition == null) return Type.OBJECT;
        if (definition.getParameters().size() != expr.getArguments().length) return Type.OBJECT;
        String name = definition.getName();
        if (resolvingFunctionReturns.contains(name)) return Type.OBJECT;
        resolvingFunctionReturns.add(name);
        try {
            TypeAnalyzer child = createFunctionBodyAnalyzer(definition, argumentTypes);
            child.analyzeNode(definition.getBody());
            return child.inferType(definition.getBody());
        } finally {
            resolvingFunctionReturns.remove(name);
        }
    }

    /**
     * 判断调用是否命中脚本内用户函数
     * 命中后即使返回 OBJECT，也不能继续套用同名系统函数的返回类型。
     */
    public boolean hasUserFunctionDefinition(FunctionCallExpression expr) {
        FunctionDefinition definition = functionDefinitions.get(expr.getFunctionName());
        return definition != null && definition.getParameters().size() == expr.getArguments().length;
    }

    private void registerFunctionDefinition(FunctionDefinition definition) {
        functionDefinitions.put(definition.getName(), definition);
    }

    private TypeAnalyzer createFunctionBodyAnalyzer(FunctionDefinition definition, Type[] argumentTypes) {
        TypeAnalyzer child = new TypeAnalyzer();
        child.rootVariableTypes = rootVariableTypes;
        child.inferredRootVariableTypes.putAll(inferredRootVariableTypes);
        child.functionDefinitions.putAll(functionDefinitions);
        child.resolvingFunctionReturns.addAll(resolvingFunctionReturns);
        for (Map.Entry<Integer, Class<?>> entry : definition.getParameterTypes().entrySet()) {
            child.forceType(entry.getKey(), Type.fromClass(entry.getValue()));
        }
        if (argumentTypes == null) return child;
        int index = 0;
        for (Integer position : definition.getParameters().values()) {
            if (index >= argumentTypes.length) break;
            if (!definition.getParameterTypes().containsKey(position)) {
                child.forceType(position, argumentTypes[index]);
            }
            index++;
        }
        return child;
    }

    public Integer inferIntConstant(ParseResult expr) {
        Object value = inferConstant(expr);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) {
            long longValue = (Long) value;
            if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                return (int) longValue;
            }
        }
        return null;
    }

    public Object inferConstant(ParseResult expr) {
        if (expr instanceof IntLiteral) {
            return ((IntLiteral) expr).getValue();
        }
        if (expr instanceof LongLiteral) {
            return ((LongLiteral) expr).getValue();
        }
        if (expr instanceof ReferenceExpression) {
            ReferenceExpression reference = (ReferenceExpression) expr;
            int position = reference.getPosition();
            if (position >= 0) {
                return localConstants.get(position);
            }
            return rootConstants.get(reference.getIdentifier().getValue());
        }
        return null;
    }

    public void recordLocalConstant(int position, Object value) {
        if (value == null) {
            localConstants.remove(position);
        } else {
            localConstants.put(position, value);
        }
    }

    public void recordRootConstant(String name, Object value) {
        if (value == null) {
            rootConstants.remove(name);
        } else {
            rootConstants.put(name, value);
        }
    }

    /**
     * 记录变量类型，处理类型合并
     */
    public void recordType(int position, Type valueType) {
        Type existing = variableTypes.get(position);
        if (existing == null) {
            variableTypes.put(position, valueType);
        } else if (!existing.equals(valueType)) {
            variableTypes.put(position, mergeTypes(existing, valueType));
        }
    }

    /**
     * 记录脚本内 root 变量类型，外部声明类型仍由 getRootVariableType 优先返回
     */
    public void recordRootType(String name, Type valueType) {
        Type existing = inferredRootVariableTypes.get(name);
        if (existing == null) {
            inferredRootVariableTypes.put(name, valueType);
        } else if (!existing.equals(valueType)) {
            inferredRootVariableTypes.put(name, mergeTypes(existing, valueType));
        }
    }

    /**
     * 强制设置变量类型（不合并，直接覆盖）
     * 用于循环变量等类型由上下文完全决定的场景
     */
    public void forceType(int position, Type valueType) {
        variableTypes.put(position, valueType);
    }

    /**
     * 合并两个类型
     */
    private Type mergeTypes(Type a, Type b) {
        if (a.isPrimitive() && b.isPrimitive()) {
            return promotePrimitive(a, b);
        }
        // 如果容器类型相同，尝试合并元素类型
        if (a.getSource().equals(b.getSource())) {
            Type elemA = a.getElementType();
            Type elemB = b.getElementType();
            if (elemA != null && elemB != null) {
                Type mergedElem = elemA.equals(elemB) ? elemA : Type.OBJECT;
                return a.withElementType(mergedElem);
            }
        }
        return Type.OBJECT;
    }

    private Type promotePrimitive(Type a, Type b) {
        if (a == Type.D || b == Type.D) return Type.D;
        if (a == Type.F || b == Type.F) return Type.F;
        if (a == Type.J || b == Type.J) return Type.J;
        if (a == Type.I && b == Type.I) return Type.I;
        return Type.OBJECT;
    }

    /**
     * 推断二元运算结果类型
     */
    public Type inferBinaryResultType(Type left, Type right, TokenType op) {
        if (isComparisonOp(op)) return Type.Z;
        if (op == TokenType.PLUS && (left == Type.STRING || right == Type.STRING)) return Type.STRING;
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
