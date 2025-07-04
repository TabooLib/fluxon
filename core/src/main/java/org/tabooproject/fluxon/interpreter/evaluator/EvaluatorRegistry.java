package org.tabooproject.fluxon.interpreter.evaluator;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.interpreter.evaluator.expr.*;
import org.tabooproject.fluxon.interpreter.evaluator.expr.literal.*;
import org.tabooproject.fluxon.interpreter.evaluator.stmt.*;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.parser.statement.StatementType;

import java.util.EnumMap;

/**
 * 求值器注册表
 */
@SuppressWarnings("unchecked")
public class EvaluatorRegistry {

    // 单例实例
    private static final EvaluatorRegistry INSTANCE = new EvaluatorRegistry();

    // 表达式求值器列表
    private final EnumMap<ExpressionType, ExpressionEvaluator<Expression>> expressions = new EnumMap<>(ExpressionType.class);
    // 语句求值器列表
    private final EnumMap<StatementType, StatementEvaluator<Statement>> statements = new EnumMap<>(StatementType.class);

    /**
     * 获取单例实例
     *
     * @return ExpressionRegistry 实例
     */
    public static EvaluatorRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 私有构造函数
     * 初始化内置求值器
     */
    private EvaluatorRegistry() {
        registerBuiltInExpressions();
        registerBuiltInStatements();
    }

    /**
     * 注册内置表达式求值器
     */
    private void registerBuiltInExpressions() {
        registerExpression(new AssignmentEvaluator());
        registerExpression(new AwaitEvaluator());
        registerExpression(new BinaryEvaluator());
        registerExpression(new ElvisEvaluator());
        registerExpression(new ForEvaluator());
        registerExpression(new FunctionCallEvaluator());
        registerExpression(new GroupingEvaluator());
        registerExpression(new IfEvaluator());
        registerExpression(new ListEvaluator());
        registerExpression(new LogicalEvaluator());
        registerExpression(new MapEvaluator());
        registerExpression(new RangeEvaluator());
        registerExpression(new ReferenceEvaluator());
        registerExpression(new UnaryEvaluator());
        registerExpression(new WhenEvaluator());
        registerExpression(new WhileEvaluator());
        // 注册字面量求值器
        registerExpression(new IdentifierEvaluator());
        registerExpression(new IntLiteralEvaluator());
        registerExpression(new NullLiteralEvaluator());
        registerExpression(new StringLiteralEvaluator());
        registerExpression(new LongLiteralEvaluator());
        registerExpression(new FloatLiteralEvaluator());
        registerExpression(new DoubleLiteralEvaluator());
        registerExpression(new BooleanLiteralEvaluator());
        registerExpression(new ContextCallEvaluator());
    }

    /**
     * 注册内置语句求值器
     */
    private void registerBuiltInStatements() {
        registerStatement(new BlockEvaluator());
        registerStatement(new ExprStmtEvaluator());
        registerStatement(new ReturnEvaluator());
        registerStatement(new BreakEvaluator());
        registerStatement(new ContinueEvaluator());
    }

    /**
     * 注册自定义表达式求值器
     */
    public void registerExpression(ExpressionEvaluator<?> expression) {
        expressions.put(expression.getType(), (ExpressionEvaluator<Expression>) expression);
    }

    /**
     * 注册自定义语句求值器
     */
    public void registerStatement(StatementEvaluator<?> statement) {
        statements.put(statement.getType(), (StatementEvaluator<Statement>) statement);
    }

    /**
     * 获取表达式求值器
     */
    @Nullable
    public ExpressionEvaluator<Expression> getExpression(ExpressionType type) {
        return expressions.get(type);
    }

    /**
     * 获取语句求值器
     */
    @Nullable
    public StatementEvaluator<Statement> getStatement(StatementType type) {
        return statements.get(type);
    }

    /**
     * 根据 [ParseResult] 获取求值器
     */
    @Nullable
    public <T extends ParseResult> Evaluator<T> getEvaluator(ParseResult result) {
        if (result instanceof Expression) {
            return (Evaluator<T>) getExpression(((Expression) result).getExpressionType());
        } else if (result instanceof Statement) {
            return (Evaluator<T>) getStatement(((Statement) result).getStatementType());
        }
        return null;
    }
}
