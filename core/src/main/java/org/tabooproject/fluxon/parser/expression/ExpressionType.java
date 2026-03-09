package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.expr.*;
import org.tabooproject.fluxon.interpreter.evaluator.expr.literal.*;
import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 表达式类型枚举
 *
 * @author sky
 */
public enum ExpressionType {

    NULL(new NullLiteralEvaluator()),
    IDENTIFIER(new IdentifierEvaluator()),
    INT_LITERAL(new IntLiteralEvaluator()),
    LONG_LITERAL(new LongLiteralEvaluator()),
    FLOAT_LITERAL(new FloatLiteralEvaluator()),
    DOUBLE_LITERAL(new DoubleLiteralEvaluator()),
    STRING_LITERAL(new StringLiteralEvaluator()),
    STRING_INTERPOLATION(new StringInterpolationEvaluator()),
    BOOLEAN_LITERAL(new BooleanLiteralEvaluator()),
    MAP(new MapEvaluator()),
    LIST(new ListEvaluator()),
    RANGE(new RangeEvaluator()),
    UNARY(new UnaryEvaluator()),
    BINARY(new BinaryEvaluator()),
    IS_CHECK(new IsEvaluator()),
    ASSIGNMENT(new AssignmentEvaluator()),
    DESTRUCTURING_ASSIGNMENT(new DestructuringAssignmentEvaluator()),
    FUNCTION_CALL(new FunctionCallEvaluator()),
    INDEX_ACCESS(new IndexAccessEvaluator()),
    AWAIT(new AwaitEvaluator()),
    REFERENCE(new ReferenceEvaluator()),
    GROUPING(new GroupingEvaluator()),
    MEMBER_ACCESS(new MemberAccessEvaluator()),
    COMMAND(new CommandEvaluator()),
    NEW(new NewEvaluator()),
    STATIC_ACCESS(new StaticAccessEvaluator()),
    IF(new IfEvaluator()),
    FOR(new ForEvaluator()),
    WHEN(new WhenEvaluator()),
    WHILE(new WhileEvaluator()),
    TRY(new TryEvaluator()),
    LOGICAL(new LogicalEvaluator()),
    TERNARY(new TernaryEvaluator()),
    ELVIS(new ElvisEvaluator()),
    CONTEXT_CALL(new ContextCallEvaluator()),
    LAMBDA(new LambdaEvaluator()),
    DOMAIN(new DomainEvaluator()),
    ANONYMOUS_CLASS(new AnonymousClassEvaluator());

    public final Evaluator<ParseResult> evaluator;

    @SuppressWarnings("unchecked")
    ExpressionType(Evaluator<?> evaluator) {
        this.evaluator = (Evaluator<ParseResult>) evaluator;
    }
}
