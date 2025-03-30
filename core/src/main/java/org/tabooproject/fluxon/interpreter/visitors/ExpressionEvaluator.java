package org.tabooproject.fluxon.interpreter.visitors;

import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.util.NumberOperations;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definitions.Definition;
import org.tabooproject.fluxon.parser.expressions.*;
import org.tabooproject.fluxon.parser.statements.Statement;
import org.tabooproject.fluxon.interpreter.destructure.DestructuringRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 表达式求值器
 * 处理所有表达式类型的求值
 */
public class ExpressionEvaluator extends AbstractVisitor {
    /**
     * 构造函数
     *
     * @param interpreter 解释器实例
     * @param environment 当前环境
     */
    public ExpressionEvaluator(Interpreter interpreter, Environment environment) {
        super(interpreter, environment);
    }

    /**
     * 访问并评估表达式
     *
     * @param expression 表达式对象
     * @return 求值结果
     */
    @Override
    public Object visitExpression(Expression expression) {
        // 使用 switch 根据表达式类型进行分派
        switch (expression.getExpressionType()) {
            // 标识符
            case IDENTIFIER:
                return ((Identifier) expression).getValue();
            // 字符串
            case STRING_LITERAL:
                return ((StringLiteral) expression).getValue();
            // 整型
            case INT_LITERAL:
                return ((IntLiteral) expression).getValue();
            // 长整型
            case LONG_LITERAL:
                return ((LongLiteral) expression).getValue();
            // 单精度
            case FLOAT_LITERAL:
                return ((FloatLiteral) expression).getValue();
            // 双精度
            case DOUBLE_LITERAL:
                return ((DoubleLiteral) expression).getValue();
            // 布尔值
            case BOOLEAN_LITERAL:
                return ((BooleanLiteral) expression).getValue();
            // 空值
            case NULL_LITERAL:
                return null;

            // 列表
            case LIST_LITERAL:
                return evaluateList((ListLiteral) expression);
            // 字典
            case MAP_LITERAL:
                return evaluateMap((MapLiteral) expression);
            // 范围
            case RANGE:
                return evaluateRange((RangeExpression) expression);

            // 表达式
            case IF:
                return evaluateIf((IfExpression) expression);
            case FOR:
                return evaluateFor((ForExpression) expression);
            case WHEN:
                return evaluateWhen((WhenExpression) expression);
            case WHILE:
                return evaluateWhile((WhileExpression) expression);

            // 一元运算
            case UNARY:
                return evaluateUnary((UnaryExpression) expression);
            // 二元运算
            case BINARY:
                return evaluateBinary((BinaryExpression) expression);
            // 逻辑运算
            case LOGICAL:
                return evaluateLogical((LogicalExpression) expression);

            // 赋值运算
            case ASSIGNMENT:
                return evaluateAssignment((Assignment) expression);
            // 函数调用
            case FUNCTION_CALL:
                return evaluateCall((FunctionCall) expression);

            // 等待
            case AWAIT:
                return evaluateAwait((AwaitExpression) expression);
            // 引用
            case REFERENCE:
                return evaluateReference((ReferenceExpression) expression);
            // Elvis
            case ELVIS:
                return evaluateElvis((ElvisExpression) expression);
            // 分组
            case GROUPING:
                return interpreter.evaluate(((GroupingExpression) expression).getExpression());
            default:
                throw new RuntimeException("Unknown expression type: " + expression.getClass().getName());
        }
    }

    /**
     * 评估列表
     *
     * @param expression 列表表达式
     * @return 列表对象
     */
    private Object evaluateList(ListLiteral expression) {
        List<Object> elements = new ArrayList<>();
        for (ParseResult element : expression.getElements()) {
            elements.add(interpreter.evaluate(element));
        }
        return elements;
    }

    /**
     * 评估字典
     *
     * @param expression 字典表达式
     * @return 字典对象
     */
    private Object evaluateMap(MapLiteral expression) {
        Map<Object, Object> entries = new HashMap<>();
        for (MapLiteral.MapEntry entry : expression.getEntries()) {
            Object key = interpreter.evaluate(entry.getKey());
            Object value = interpreter.evaluate(entry.getValue());
            entries.put(key, value);
        }
        return entries;
    }

    /**
     * 评估范围表达式
     *
     * @param expression 范围表达式
     * @return 范围对象（通常是整数列表）
     */
    private Object evaluateRange(RangeExpression expression) {
        // 获取开始值和结束值
        Object start = interpreter.evaluate(expression.getStart());
        Object end = interpreter.evaluate(expression.getEnd());

        // 检查开始值和结束值是否为数字
        if (!(start instanceof Number) || !(end instanceof Number)) {
            throw new RuntimeException("Range bounds must be numbers");
        }

        // 转换为整数
        int startInt = ((Number) start).intValue();
        int endInt = ((Number) end).intValue();

        // 检查范围是否为包含上界类型
        boolean isInclusive = expression.isInclusive();
        if (!isInclusive) {
            endInt--;
        }

        // 创建范围结果列表
        List<Integer> result = new ArrayList<>();
        // 支持正向和反向范围
        if (startInt <= endInt) {
            // 正向范围
            for (int i = startInt; i <= endInt; i++) {
                result.add(i);
            }
        } else {
            // 反向范围
            for (int i = startInt; i >= endInt; i--) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * 评估一元表达式
     *
     * @param expression 一元表达式
     * @return 求值结果
     */
    private Object evaluateUnary(UnaryExpression expression) {
        Object right = interpreter.evaluate(expression.getRight());

        switch (expression.getOperator().getType()) {
            case NOT:
                return !isTrue(right);
            case MINUS:
                checkNumberOperand(right);
                return NumberOperations.negateNumber(right);
            default:
                throw new RuntimeException("Unknown unary operator: " + expression.getOperator().getType());
        }
    }

    /**
     * 评估二元表达式
     *
     * @param expression 二元表达式
     * @return 求值结果
     */
    private Object evaluateBinary(BinaryExpression expression) {
        Object left = interpreter.evaluate(expression.getLeft());
        Object right = interpreter.evaluate(expression.getRight());

        switch (expression.getOperator().getType()) {
            case MINUS:
                checkNumberOperands(left, right);
                return NumberOperations.subtractNumbers(left, right);
            case DIVIDE:
                checkNumberOperands(left, right);
                return NumberOperations.divideNumbers(left, right);
            case MULTIPLY:
                checkNumberOperands(left, right);
                return NumberOperations.multiplyNumbers(left, right);
            case PLUS:
                if (left instanceof Number && right instanceof Number) {
                    return NumberOperations.addNumbers(left, right);
                }
                if (left instanceof String || right instanceof String) {
                    return String.valueOf(left) + right;
                }
                throw new RuntimeException("Operands must be numbers or strings.");
            case MODULO:
                checkNumberOperands(left, right);
                return NumberOperations.moduloNumbers(left, right);
            case GREATER:
                checkNumberOperands(left, right);
                return NumberOperations.compareNumbers(left, right) > 0;
            case GREATER_EQUAL:
                checkNumberOperands(left, right);
                return NumberOperations.compareNumbers(left, right) >= 0;
            case LESS:
                checkNumberOperands(left, right);
                return NumberOperations.compareNumbers(left, right) < 0;
            case LESS_EQUAL:
                checkNumberOperands(left, right);
                return NumberOperations.compareNumbers(left, right) <= 0;
            case EQUAL:
                return isEqual(left, right);
            case NOT_EQUAL:
                return !isEqual(left, right);
            default:
                throw new RuntimeException("Unknown binary operator: " + expression.getOperator().getType());
        }
    }

    /**
     * 评估逻辑表达式
     *
     * @param expression 逻辑表达式
     * @return 求值结果
     */
    private Object evaluateLogical(LogicalExpression expression) {
        Object left = interpreter.evaluate(expression.getLeft());
        // 逻辑或
        if (expression.getOperator().getType() == TokenType.OR) {
            if (isTrue(left)) return left;
        } else {
            if (!isTrue(left)) return left;
        }
        return interpreter.evaluate(expression.getRight());
    }

    /**
     * 评估赋值表达式
     *
     * @param expression 赋值表达式
     * @return 赋值结果
     */
    private Object evaluateAssignment(Assignment expression) {
        Object value = interpreter.evaluate(expression.getValue());

        // 根据赋值操作符类型处理赋值
        if (expression.getOperator().getType() == TokenType.ASSIGN) {
            environment.assign(expression.getName(), value);
        } else {
            // 处理复合赋值
            Object current = environment.get(expression.getName());
            if (!(current instanceof Number) || !(value instanceof Number)) {
                throw new RuntimeException("Operands for compound assignment must be numbers.");
            }
            // 根据操作符类型进行不同的复合赋值操作
            switch (expression.getOperator().getType()) {
                case PLUS_ASSIGN:
                    value = NumberOperations.addNumbers(current, value);
                    break;
                case MINUS_ASSIGN:
                    value = NumberOperations.subtractNumbers(current, value);
                    break;
                case MULTIPLY_ASSIGN:
                    value = NumberOperations.multiplyNumbers(current, value);
                    break;
                case DIVIDE_ASSIGN:
                    value = NumberOperations.divideNumbers(current, value);
                    break;
                case MODULO_ASSIGN:
                    value = NumberOperations.moduloNumbers(current, value);
                    break;
                default:
                    throw new RuntimeException("Unknown compound assignment operator: " + expression.getOperator().getType());
            }
            environment.assign(expression.getName(), value);
        }
        return value;
    }

    /**
     * 评估函数调用
     *
     * @param expression 函数调用表达式
     * @return 调用结果
     */
    private Object evaluateCall(FunctionCall expression) {
        // 评估被调用者
        Object callee = interpreter.evaluate(expression.getCallee());

        // 评估参数列表
        Object[] arguments = new Object[expression.getArguments().size()];
        List<ParseResult> expressionArguments = expression.getArguments();
        for (int i = 0; i < expressionArguments.size(); i++) {
            ParseResult argument = expressionArguments.get(i);
            arguments[i] = interpreter.evaluate(argument);
        }

        // 如果被调用者是一个函数
        if (callee instanceof Function) {
            return ((Function) callee).call(arguments);
        } else {
            return environment.getFunction(callee.toString()).call(arguments);
        }
    }

    /**
     * 评估 await 表达式
     *
     * @param expression await 表达式
     * @return 评估结果
     */
    private Object evaluateAwait(AwaitExpression expression) {
        return null;
    }

    /**
     * 评估引用表达式
     *
     * @param expression 引用表达式
     * @return 引用结果
     */
    private Object evaluateReference(ReferenceExpression expression) {
        return environment.get(expression.getIdentifier().getValue());
    }

    /**
     * 评估 Elvis 表达式
     */
    private Object evaluateElvis(ElvisExpression expression) {
        Object object = interpreter.evaluate(expression.getCondition());
        if (object == null) {
            return interpreter.evaluate(expression.getAlternative());
        }
        return object;
    }

    /**
     * 评估 if 表达式
     *
     * @param expression if 表达式
     * @return 评估结果
     */
    private Object evaluateIf(IfExpression expression) {
        if (isTrue(interpreter.evaluate(expression.getCondition()))) {
            return interpreter.evaluate(expression.getThenBranch());
        } else if (expression.getElseBranch() != null) {
            return interpreter.evaluate(expression.getElseBranch());
        } else {
            return null;
        }
    }

    /**
     * 评估 For 表达式
     *
     * @param expression for 表达式
     * @return 评估结果，通常是最后一次迭代的结果，或 null
     */
    private Object evaluateFor(ForExpression expression) {
        // 评估集合表达式
        Object collection = interpreter.evaluate(expression.getCollection());
        
        // 创建迭代器，根据集合类型进行不同处理
        Iterator<?> iterator;
        
        if (collection instanceof List) {
            iterator = ((List<?>) collection).iterator();
        } else if (collection instanceof Map) {
            iterator = ((Map<?, ?>) collection).entrySet().iterator();
        } else if (collection instanceof Iterable) {
            iterator = ((Iterable<?>) collection).iterator();
        } else if (collection instanceof Object[]) {
            iterator = Arrays.asList((Object[]) collection).iterator();
        } else if (collection != null) {
            throw new RuntimeException("Cannot iterate over " + collection.getClass().getName());
        } else {
            throw new RuntimeException("Cannot iterate over null");
        }
        
        // 获取变量名列表
        List<String> variables = expression.getVariables();
        
        // 记录最后一次迭代的结果
        Object result = null;
        
        // 创建新环境进行迭代
        Environment previousEnv = environment;
        environment = new Environment(previousEnv);
        
        try {
            // 迭代集合元素
            while (iterator.hasNext()) {
                // 使用解构器注册表执行解构
                DestructuringRegistry.getInstance().destructure(environment, variables, iterator.next());
                // 执行循环体
                result = interpreter.evaluate(expression.getBody());
            }
        } finally {
            // 恢复环境
            environment = previousEnv;
        }
        return result;
    }

    /**
     * 评估 when 表达式
     *
     * @param expression when 表达式
     * @return 评估结果
     */
    private Object evaluateWhen(WhenExpression expression) {
        // 获取并评估主题对象（如果有）
        Object subject = null;
        if (expression.getSubject() != null) {
            subject = interpreter.evaluate(expression.getSubject());
        }

        // 遍历所有分支
        for (WhenExpression.WhenBranch branch : expression.getBranches()) {
            // 如果是 else 分支（没有条件），直接返回其结果
            if (branch.getCondition() == null) {
                return interpreter.evaluate(branch.getResult());
            }

            // 评估分支条件
            Object condition = interpreter.evaluate(branch.getCondition());
            boolean matches = false;

            // 根据匹配类型进行判断
            switch (branch.getMatchType()) {
                case EQUAL:
                    // 如果有主题，判断主题和条件是否相等
                    if (subject != null) {
                        matches = isEqual(subject, condition);
                    } else {
                        // 没有主题时，直接判断条件是否为真
                        matches = isTrue(condition);
                    }
                    break;
                // 判断主题是否包含在条件中
                case CONTAINS:
                    if (subject != null && condition != null) {
                        if (condition instanceof List) {
                            matches = ((List<?>) condition).contains(subject);
                        } else if (condition instanceof Map) {
                            matches = ((Map<?, ?>) condition).containsKey(subject);
                        } else if (condition instanceof String && subject instanceof String) {
                            matches = ((String) condition).contains((String) subject);
                        }
                    }
                    break;
                // 判断主题是否不包含在条件中
                case NOT_CONTAINS:
                    if (subject != null && condition != null) {
                        if (condition instanceof List) {
                            matches = !((List<?>) condition).contains(subject);
                        } else if (condition instanceof Map) {
                            matches = !((Map<?, ?>) condition).containsKey(subject);
                        } else if (condition instanceof String && subject instanceof String) {
                            matches = !((String) condition).contains((String) subject);
                        }
                    }
                    break;
            }
            // 如果匹配成功，执行对应分支的结果
            if (matches) {
                return interpreter.evaluate(branch.getResult());
            }
        }
        // 如果没有匹配的分支，返回 null
        return null;
    }

    /**
     * 评估 while 表达式
     *
     * @param expression while 表达式
     * @return 评估结果
     */
    private Object evaluateWhile(WhileExpression expression) {
        Object result = null;
        while (isTrue(interpreter.evaluate(expression.getCondition()))) {
            result = interpreter.evaluate(expression.getBody());
        }
        return result;
    }

    /**
     * 检查操作数是否为数字
     *
     * @param operand 操作数
     */
    private void checkNumberOperand(Object operand) {
        if (operand instanceof Number) return;
        throw new RuntimeException("Operands must be numbers.");
    }

    /**
     * 检查操作数是否都是数字
     *
     * @param left  左操作数
     * @param right 右操作数
     */
    private void checkNumberOperands(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) return;
        throw new RuntimeException("Operands must be numbers.");
    }

    /**
     * 不支持的其他类型的 visit 方法
     */
    @Override
    public Object visitStatement(Statement statement) {
        throw new UnsupportedOperationException("Expression evaluator does not support evaluating statements.");
    }

    @Override
    public Object visitDefinition(Definition definition) {
        throw new UnsupportedOperationException("Expression evaluator does not support evaluating definitions.");
    }
} 