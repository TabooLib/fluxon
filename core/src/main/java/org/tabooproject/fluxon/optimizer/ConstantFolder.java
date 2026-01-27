package org.tabooproject.fluxon.optimizer;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.parser.expression.GroupingExpression;
import org.tabooproject.fluxon.parser.expression.IfExpression;
import org.tabooproject.fluxon.parser.expression.TernaryExpression;
import org.tabooproject.fluxon.parser.expression.UnaryExpression;
import org.tabooproject.fluxon.parser.expression.literal.BooleanLiteral;
import org.tabooproject.fluxon.parser.expression.literal.DoubleLiteral;
import org.tabooproject.fluxon.parser.expression.literal.FloatLiteral;
import org.tabooproject.fluxon.parser.expression.literal.IntLiteral;
import org.tabooproject.fluxon.parser.expression.literal.Literal;
import org.tabooproject.fluxon.parser.expression.literal.LongLiteral;
import org.tabooproject.fluxon.parser.expression.literal.StringLiteral;
import org.tabooproject.fluxon.parser.statement.Block;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;

import java.util.ArrayList;
import java.util.List;

/**
 * 常量折叠优化器
 * <p>
 * 在编译时计算常量表达式，将 1 + 2 优化为 3，减少运行时开销。
 *
 * @author sky
 */
public class ConstantFolder {

    /**
     * 批量折叠
     */
    public List<ParseResult> foldAll(List<ParseResult> results) {
        List<ParseResult> folded = new ArrayList<>(results.size());
        for (ParseResult result : results) {
            folded.add(fold(result));
        }
        return folded;
    }

    /**
     * 折叠单个节点
     */
    public ParseResult fold(ParseResult node) {
        if (node == null) {
            return null;
        }
        // 字面量直接返回
        if (node instanceof Literal) {
            return node;
        }
        // 可计算的表达式
        if (node instanceof BinaryExpression) {
            return foldBinary((BinaryExpression) node);
        }
        if (node instanceof UnaryExpression) {
            return foldUnary((UnaryExpression) node);
        }
        if (node instanceof GroupingExpression) {
            return foldGrouping((GroupingExpression) node);
        }
        // 语句类型
        if (node instanceof ExpressionStatement) {
            return foldExpressionStatement((ExpressionStatement) node);
        }
        if (node instanceof Block) {
            return foldBlock((Block) node);
        }
        // 表达式类型
        if (node instanceof AssignExpression) {
            return foldAssign((AssignExpression) node);
        }
        if (node instanceof TernaryExpression) {
            return foldTernary((TernaryExpression) node);
        }
        if (node instanceof IfExpression) {
            return foldIf((IfExpression) node);
        }
        if (node instanceof FunctionCallExpression) {
            return foldFunctionCall((FunctionCallExpression) node);
        }
        // 其他节点不处理
        return node;
    }

    private ParseResult foldExpressionStatement(ExpressionStatement stmt) {
        ParseResult folded = fold(stmt.getExpression());
        if (folded != stmt.getExpression()) {
            return new ExpressionStatement(folded);
        }
        return stmt;
    }

    private ParseResult foldBlock(Block block) {
        ParseResult[] stmts = block.getStatements();
        ParseResult[] folded = foldArray(stmts);
        if (folded != stmts) {
            return new Block(block.getLabel(), folded);
        }
        return block;
    }

    private ParseResult foldAssign(AssignExpression expr) {
        ParseResult foldedValue = fold(expr.getValue());
        if (foldedValue != expr.getValue()) {
            return new AssignExpression(expr.getTarget(), expr.getOperator(), foldedValue, expr.getPosition());
        }
        return expr;
    }

    private ParseResult foldTernary(TernaryExpression expr) {
        ParseResult cond = fold(expr.getCondition());
        ParseResult trueExpr = fold(expr.getTrueExpr());
        ParseResult falseExpr = fold(expr.getFalseExpr());
        // 如果条件是布尔字面量，可以直接返回对应分支
        if (cond instanceof BooleanLiteral) {
            return ((BooleanLiteral) cond).getValue() ? trueExpr : falseExpr;
        }
        if (cond != expr.getCondition() || trueExpr != expr.getTrueExpr() || falseExpr != expr.getFalseExpr()) {
            return new TernaryExpression(cond, trueExpr, falseExpr);
        }
        return expr;
    }

    private ParseResult foldIf(IfExpression expr) {
        ParseResult cond = fold(expr.getCondition());
        ParseResult thenBranch = fold(expr.getThenBranch());
        ParseResult elseBranch = expr.getElseBranch() != null ? fold(expr.getElseBranch()) : null;
        if (cond != expr.getCondition() || thenBranch != expr.getThenBranch() ||
            (expr.getElseBranch() != null && elseBranch != expr.getElseBranch())) {
            return new IfExpression(cond, thenBranch, elseBranch);
        }
        return expr;
    }

    private ParseResult foldFunctionCall(FunctionCallExpression expr) {
        ParseResult[] args = expr.getArguments();
        ParseResult[] foldedArgs = foldArray(args);
        if (foldedArgs != args) {
            return new FunctionCallExpression(expr.getFunctionName(), foldedArgs, expr.getPosition(), expr.getExtensionPosition());
        }
        return expr;
    }

    private ParseResult[] foldArray(ParseResult[] children) {
        if (children == null || children.length == 0) {
            return children;
        }
        ParseResult[] result = null;
        for (int i = 0; i < children.length; i++) {
            ParseResult folded = fold(children[i]);
            if (folded != children[i]) {
                if (result == null) {
                    result = children.clone();
                }
                result[i] = folded;
            }
        }
        return result != null ? result : children;
    }

    /**
     * 折叠二元表达式
     */
    private ParseResult foldBinary(BinaryExpression expr) {
        ParseResult left = fold(expr.getLeft());
        ParseResult right = fold(expr.getRight());
        // 两边都是字面量才能完全折叠
        if (left instanceof Literal && right instanceof Literal) {
            ParseResult computed = tryComputeBinary((Literal) left, (Literal) right, expr.getOperator().getType());
            if (computed != null) {
                return computed;
            }
        }
        // 部分折叠：如果子树有变化则返回新节点
        if (left != expr.getLeft() || right != expr.getRight()) {
            return new BinaryExpression(left, expr.getOperator(), right);
        }
        return expr;
    }

    private ParseResult tryComputeBinary(Literal leftLit, Literal rightLit, TokenType op) {
        // 字符串拼接
        if (op == TokenType.PLUS && leftLit instanceof StringLiteral && rightLit instanceof StringLiteral) {
            return new StringLiteral(((StringLiteral) leftLit).getValue() + ((StringLiteral) rightLit).getValue());
        }
        // 数值运算
        Object leftVal = leftLit.getSourceValue();
        Object rightVal = rightLit.getSourceValue();
        if (leftVal instanceof Number && rightVal instanceof Number) {
            return computeBinary((Number) leftVal, (Number) rightVal, op);
        }
        // 布尔比较
        if (leftVal instanceof Boolean && rightVal instanceof Boolean) {
            return computeBooleanBinary((Boolean) leftVal, (Boolean) rightVal, op);
        }
        return null;
    }

    /**
     * 折叠一元表达式
     */
    private ParseResult foldUnary(UnaryExpression expr) {
        ParseResult operand = fold(expr.getRight());
        if (operand instanceof Literal) {
            Literal lit = (Literal) operand;
            TokenType op = expr.getOperator().getType();
            Object val = lit.getSourceValue();
            if (op == TokenType.MINUS && val instanceof Number) {
                ParseResult result = computeUnaryMinus((Number) val);
                if (result != null) return result;
            }
            if (op == TokenType.NOT && val instanceof Boolean) {
                return new BooleanLiteral(!(Boolean) val);
            }
        }
        if (operand != expr.getRight()) {
            return new UnaryExpression(expr.getOperator(), operand);
        }
        return expr;
    }

    /**
     * 折叠分组表达式
     */
    private ParseResult foldGrouping(GroupingExpression expr) {
        ParseResult inner = fold(expr.getExpression());
        if (inner instanceof Literal) {
            return inner;
        }
        if (inner != expr.getExpression()) {
            return new GroupingExpression(inner);
        }
        return expr;
    }

    private ParseResult computeBinary(Number left, Number right, TokenType op) {
        int commonType = promoteType(left, right);
        // 幂运算始终返回 double
        if (op == TokenType.POWER) {
            return new DoubleLiteral(Math.pow(left.doubleValue(), right.doubleValue()));
        }
        // 整数除零检查：不折叠
        if ((op == TokenType.DIVIDE || op == TokenType.MODULO) && commonType <= 1) {
            if (commonType == 0 && right.intValue() == 0) return null;
            if (commonType == 1 && right.longValue() == 0) return null;
        }
        switch (op) {
            case PLUS:
            case MINUS:
            case MULTIPLY:
            case DIVIDE:
            case MODULO:
                return computeArithmetic(left, right, op, commonType);
            case GREATER:
            case GREATER_EQUAL:
            case LESS:
            case LESS_EQUAL:
            case EQUAL:
            case NOT_EQUAL:
                return computeComparison(left, right, op, commonType);
            default:
                return null;
        }
    }

    private ParseResult computeArithmetic(Number left, Number right, TokenType op, int commonType) {
        switch (commonType) {
            case 3: return new DoubleLiteral(arithDouble(left.doubleValue(), right.doubleValue(), op));
            case 2: return new FloatLiteral(arithFloat(left.floatValue(), right.floatValue(), op));
            case 1: return new LongLiteral(arithLong(left.longValue(), right.longValue(), op));
            default: return new IntLiteral(arithInt(left.intValue(), right.intValue(), op));
        }
    }

    private double arithDouble(double l, double r, TokenType op) {
        switch (op) {
            case PLUS: return l + r;
            case MINUS: return l - r;
            case MULTIPLY: return l * r;
            case DIVIDE: return l / r;
            case MODULO: return l % r;
            default: throw new IllegalArgumentException("Unknown op: " + op);
        }
    }

    private float arithFloat(float l, float r, TokenType op) {
        switch (op) {
            case PLUS: return l + r;
            case MINUS: return l - r;
            case MULTIPLY: return l * r;
            case DIVIDE: return l / r;
            case MODULO: return l % r;
            default: throw new IllegalArgumentException("Unknown op: " + op);
        }
    }

    private long arithLong(long l, long r, TokenType op) {
        switch (op) {
            case PLUS: return l + r;
            case MINUS: return l - r;
            case MULTIPLY: return l * r;
            case DIVIDE: return l / r;
            case MODULO: return l % r;
            default: throw new IllegalArgumentException("Unknown op: " + op);
        }
    }

    private int arithInt(int l, int r, TokenType op) {
        switch (op) {
            case PLUS: return l + r;
            case MINUS: return l - r;
            case MULTIPLY: return l * r;
            case DIVIDE: return l / r;
            case MODULO: return l % r;
            default: throw new IllegalArgumentException("Unknown op: " + op);
        }
    }

    private ParseResult computeComparison(Number left, Number right, TokenType op, int commonType) {
        // EQUAL/NOT_EQUAL 使用直接比较，正确处理 -0.0 == 0.0
        if (op == TokenType.EQUAL || op == TokenType.NOT_EQUAL) {
            boolean eq;
            if (commonType == 3) eq = left.doubleValue() == right.doubleValue();
            else if (commonType == 2) eq = left.floatValue() == right.floatValue();
            else if (commonType == 1) eq = left.longValue() == right.longValue();
            else eq = left.intValue() == right.intValue();
            return new BooleanLiteral((op == TokenType.EQUAL) == eq);
        }
        int cmp;
        if (commonType == 3) cmp = Double.compare(left.doubleValue(), right.doubleValue());
        else if (commonType == 2) cmp = Float.compare(left.floatValue(), right.floatValue());
        else if (commonType == 1) cmp = Long.compare(left.longValue(), right.longValue());
        else cmp = Integer.compare(left.intValue(), right.intValue());
        boolean result;
        switch (op) {
            case GREATER: result = cmp > 0; break;
            case GREATER_EQUAL: result = cmp >= 0; break;
            case LESS: result = cmp < 0; break;
            case LESS_EQUAL: result = cmp <= 0; break;
            default: return null;
        }
        return new BooleanLiteral(result);
    }

    private ParseResult computeBooleanBinary(Boolean left, Boolean right, TokenType op) {
        switch (op) {
            case EQUAL: return new BooleanLiteral(left.equals(right));
            case NOT_EQUAL: return new BooleanLiteral(!left.equals(right));
            default: return null;
        }
    }

    private ParseResult computeUnaryMinus(Number val) {
        if (val instanceof Integer) return new IntLiteral(-val.intValue());
        if (val instanceof Long) return new LongLiteral(-val.longValue());
        if (val instanceof Float) return new FloatLiteral(-val.floatValue());
        if (val instanceof Double) return new DoubleLiteral(-val.doubleValue());
        return null;
    }

    private int promoteType(Number a, Number b) {
        return Math.max(typeRank(a), typeRank(b));
    }

    private int typeRank(Number n) {
        if (n instanceof Double) return 3;
        if (n instanceof Float) return 2;
        if (n instanceof Long) return 1;
        return 0;
    }
}
