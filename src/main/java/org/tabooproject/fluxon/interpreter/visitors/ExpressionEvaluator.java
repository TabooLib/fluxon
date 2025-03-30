package org.tabooproject.fluxon.interpreter.visitors;

import org.tabooproject.fluxon.interpreter.Environment;
import org.tabooproject.fluxon.interpreter.Function;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.UserFunction;
import org.tabooproject.fluxon.interpreter.util.NumberOperations;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definitions.Definition;
import org.tabooproject.fluxon.parser.expressions.*;
import org.tabooproject.fluxon.parser.statements.Statement;

import java.util.ArrayList;
import java.util.List;

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
            case INT_LITERAL:
                return ((IntLiteral) expression).getValue();
            case LONG_LITERAL:
                return ((LongLiteral) expression).getValue();
            case FLOAT_LITERAL:
                return ((FloatLiteral) expression).getValue();
            case DOUBLE_LITERAL:
                return ((DoubleLiteral) expression).getValue();
            case STRING_LITERAL:
                return ((StringLiteral) expression).getValue();
            case BOOLEAN_LITERAL:
                return ((BooleanLiteral) expression).getValue();
            case IDENTIFIER:
                return evaluateIdentifier((Identifier) expression);
            case BINARY:
                return evaluateBinary((BinaryExpression) expression);
            case LOGICAL:
                return evaluateLogical((LogicalExpression) expression);
            case UNARY:
                return evaluateUnary((UnaryExpression) expression);
            case ASSIGNMENT:
                return evaluateAssignment((Assignment) expression);
            case FUNCTION_CALL:
                return evaluateCall((FunctionCall) expression);
            case AWAIT:
                return evaluateAwait((AwaitExpression) expression);
            case REFERENCE:
                return evaluateReference((ReferenceExpression) expression);
            case IF:
                return evaluateIf((IfExpression) expression);
            case WHILE:
                return evaluateWhile((WhileExpression) expression);
            case GROUPING:
                return interpreter.evaluate(((GroupingExpression) expression).getExpression());
            default:
                throw new RuntimeException("Unknown expression type: " + expression.getClass().getName());
        }
    }

    /**
     * 评估标识符
     *
     * @param identifier 标识符
     * @return 变量值
     */
    private Object evaluateIdentifier(Identifier identifier) {
        return environment.get(identifier.getName());
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
                checkNumberOperands(expression.getOperator(), left, right);
                return NumberOperations.subtractNumbers(left, right);
            case DIVIDE:
                checkNumberOperands(expression.getOperator(), left, right);
                return NumberOperations.divideNumbers(left, right);
            case MULTIPLY:
                checkNumberOperands(expression.getOperator(), left, right);
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
                checkNumberOperands(expression.getOperator(), left, right);
                return NumberOperations.moduloNumbers(left, right);
            case GREATER:
                checkNumberOperands(expression.getOperator(), left, right);
                return NumberOperations.compareNumbers(left, right) > 0;
            case GREATER_EQUAL:
                checkNumberOperands(expression.getOperator(), left, right);
                return NumberOperations.compareNumbers(left, right) >= 0;
            case LESS:
                checkNumberOperands(expression.getOperator(), left, right);
                return NumberOperations.compareNumbers(left, right) < 0;
            case LESS_EQUAL:
                checkNumberOperands(expression.getOperator(), left, right);
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

        if (expression.getOperator().getType() == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return interpreter.evaluate(expression.getRight());
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
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expression.getOperator(), right);
                return NumberOperations.negateNumber(right);
            default:
                throw new RuntimeException("Unknown unary operator: " + expression.getOperator().getType());
        }
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
        if (expression.getOperator().getType() == TokenType.EQUAL) {
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
        List<Object> arguments = new ArrayList<>();
        for (ParseResult argument : expression.getArguments()) {
            arguments.add(interpreter.evaluate(argument));
        }

        // 确保被调用者是一个函数
        if (!(callee instanceof Function)) {
            throw new RuntimeException("Only functions can be called.");
        }

        // 执行函数调用
        Function function = (Function) callee;
        // 调用函数
        return function.call(arguments.toArray());
    }

    /**
     * 评估 await 表达式
     *
     * @param expression await 表达式
     * @return 评估结果
     */
    private Object evaluateAwait(AwaitExpression expression) {
        // 直接评估表达式，未实现真正的异步处理
        return interpreter.evaluate(expression.getExpression());
    }

    /**
     * 评估引用表达式
     *
     * @param expression 引用表达式
     * @return 引用结果
     */
    private Object evaluateReference(ReferenceExpression expression) {
        // 这里只返回原始值，不实现真正的引用语义
        return interpreter.evaluate(expression.getExpression());
    }

    /**
     * 评估 if 表达式
     *
     * @param expression if 表达式
     * @return 评估结果
     */
    private Object evaluateIf(IfExpression expression) {
        if (isTruthy(interpreter.evaluate(expression.getCondition()))) {
            return interpreter.evaluate(expression.getThenBranch());
        } else if (expression.getElseBranch() != null) {
            return interpreter.evaluate(expression.getElseBranch());
        } else {
            return null;
        }
    }

    /**
     * 评估 while 表达式
     *
     * @param expression while 表达式
     * @return 评估结果
     */
    private Object evaluateWhile(WhileExpression expression) {
        Object result = null;
        while (isTruthy(interpreter.evaluate(expression.getCondition()))) {
            result = interpreter.evaluate(expression.getBody());
        }
        return result;
    }

    /**
     * 检查操作数是否为数字
     *
     * @param operator 操作符
     * @param operand  操作数
     */
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Number) return;
        throw new RuntimeException("Operands must be numbers.");
    }

    /**
     * 检查操作数是否都是数字
     *
     * @param operator 操作符
     * @param left     左操作数
     * @param right    右操作数
     */
    private void checkNumberOperands(Token operator, Object left, Object right) {
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