package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.expr.*;
import org.tabooproject.fluxon.interpreter.evaluator.expr.DestructuringAssignmentEvaluator;
import org.tabooproject.fluxon.interpreter.evaluator.expr.IsEvaluator;
import org.tabooproject.fluxon.interpreter.evaluator.expr.NewEvaluator;
import org.tabooproject.fluxon.interpreter.evaluator.expr.StaticAccessEvaluator;
import org.tabooproject.fluxon.interpreter.evaluator.expr.StringInterpolationEvaluator;
import org.tabooproject.fluxon.interpreter.evaluator.expr.AnonymousClassEvaluator;
import org.tabooproject.fluxon.interpreter.evaluator.expr.literal.*;
import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 表达式类型枚举
 * 用于区分不同类型的表达式
 */
public enum ExpressionType {

    // 空值
    NULL(new NullLiteralEvaluator()),

    // 标识符
    IDENTIFIER(new IdentifierEvaluator()),

    // 字面量
    INT_LITERAL(new IntLiteralEvaluator()),
    LONG_LITERAL(new LongLiteralEvaluator()),
    FLOAT_LITERAL(new FloatLiteralEvaluator()),
    DOUBLE_LITERAL(new DoubleLiteralEvaluator()),
    STRING_LITERAL(new StringLiteralEvaluator()),
    STRING_INTERPOLATION(new StringInterpolationEvaluator()),
    BOOLEAN_LITERAL(new BooleanLiteralEvaluator()),

    // 高级字面量
    MAP(new MapEvaluator()),
    LIST(new ListEvaluator()),
    RANGE(new RangeEvaluator()),

    // 表达式
    IF(new IfEvaluator()),
    FOR(new ForEvaluator()),
    WHEN(new WhenEvaluator()),
    WHILE(new WhileEvaluator()),
    TRY(new TryEvaluator()),

    // 一元、二元和逻辑表达式
    UNARY(new UnaryEvaluator()),
    BINARY(new BinaryEvaluator()),
    LOGICAL(new LogicalEvaluator()),
    IS_CHECK(new IsEvaluator()),

    // 赋值表达式
    ASSIGNMENT(new AssignmentEvaluator()),
    // 解构赋值表达式
    DESTRUCTURING_ASSIGNMENT(new DestructuringAssignmentEvaluator()),
    // 函数调用
    FUNCTION_CALL(new FunctionCallEvaluator()),
    // 索引访问
    INDEX_ACCESS(new IndexAccessEvaluator()),

    AWAIT(new AwaitEvaluator()),
    REFERENCE(new ReferenceEvaluator()),
    TERNARY(new TernaryEvaluator()),
    ELVIS(new ElvisEvaluator()),
    GROUPING(new GroupingEvaluator()),

    // 上下文调用
    CONTEXT_CALL(new ContextCallEvaluator()),
    // 成员访问（反射）
    MEMBER_ACCESS(new MemberAccessEvaluator()),
    // Lambda
    LAMBDA(new LambdaEvaluator()),
    // Command (自定义语法扩展)
    COMMAND(new CommandEvaluator()),
    // Domain (域语法)
    DOMAIN(new DomainEvaluator()),
    // New (Java 对象构造)
    NEW(new NewEvaluator()),
    // Static Access (Java 静态成员访问)
    STATIC_ACCESS(new StaticAccessEvaluator()),
    // Anonymous Class (匿名类表达式)
    ANONYMOUS_CLASS(new AnonymousClassEvaluator());

    public final Evaluator<ParseResult> evaluator;

    @SuppressWarnings("unchecked")
    ExpressionType(Evaluator<?> evaluator) {
        this.evaluator = (Evaluator<ParseResult>) evaluator;
    }
}
