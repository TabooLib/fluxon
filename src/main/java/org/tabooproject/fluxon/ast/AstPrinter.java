package org.tabooproject.fluxon.ast;

import org.tabooproject.fluxon.ast.expression.*;
import org.tabooproject.fluxon.ast.statement.*;

/**
 * AST 打印器
 * 用于将 AST 转换为字符串
 */
public class AstPrinter implements AstVisitor<String> {

    @Override
    public String visitBinaryExpr(BinaryExpr node) {
        return parenthesize(node.getOperator().getSymbol(), node.getLeft(), node.getRight());
    }

    @Override
    public String visitUnaryExpr(UnaryExpr node) {
        return parenthesize(node.getOperator().getSymbol(), node.getOperand());
    }

    @Override
    public String visitLiteralExpr(LiteralExpr node) {
        if (node.getValue() == null) return "null";
        return "L" + node.getValue().toString();
    }

    @Override
    public String visitVariableExpr(VariableExpr node) {
        return "&" + node.getName();
    }

    @Override
    public String visitCallExpr(CallExpr node) {
        return parenthesize(node.getName() != null ? node.getName() : "call", node.getArguments().toArray(new Expr[0]));
    }

    @Override
    public String visitIfExpr(IfExpr node) {
        if (node.getElseBranch() != null) {
            return parenthesize("if-else",
                    node.getCondition(), node.getThenBranch(), node.getElseBranch());
        }
        return parenthesize("if",
                node.getCondition(), node.getThenBranch());
    }

    @Override
    public String visitWhenExpr(WhenExpr node) {
        return "when";
    }

    @Override
    public String visitListExpr(ListExpr node) {
        return "list";
    }

    @Override
    public String visitMapExpr(MapExpr node) {
        return "map";
    }

    @Override
    public String visitRangeExpr(RangeExpr node) {
        return "range";
    }

    @Override
    public String visitSafeAccessExpr(SafeAccessExpr node) {
        return "safe-access";
    }

    @Override
    public String visitElvisExpr(ElvisExpr node) {
        return "elvis";
    }

    @Override
    public String visitLambdaExpr(LambdaExpr node) {
        return "lambda";
    }

    @Override
    public String visitBlockExpr(BlockExpr node) {
        return "block-expr: " + node.getBlock().accept(this);
    }

    @Override
    public String visitBlockStmt(BlockStmt node) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");

        for (Stmt stmt : node.getStatements()) {
            builder.append("  ").append(stmt.accept(this)).append("\n");
        }

        builder.append("}");
        return builder.toString();
    }

    @Override
    public String visitExpressionStmt(ExpressionStmt node) {
        return node.getExpression().accept(this) + ";";
    }

    @Override
    public String visitVarDeclStmt(VarDeclStmt node) {
        if (node.getInitializer() != null) {
            return (node.isVal() ? "val " : "var ") +
                    node.getName() +
                    (node.getTypeAnnotation() != null ? ": " + node.getTypeAnnotation() : "") +
                    " = " +
                    node.getInitializer().accept(this);
        }

        return (node.isVal() ? "val " : "var ") +
                node.getName() +
                (node.getTypeAnnotation() != null ? ": " + node.getTypeAnnotation() : "");
    }

    @Override
    public String visitFunctionDeclStmt(FunctionDeclStmt node) {
        StringBuilder builder = new StringBuilder();

        if (node.isAsync()) {
            builder.append("async ");
        }

        builder.append("def ").append(node.getName()).append("(");

        for (int i = 0; i < node.getParameters().size(); i++) {
            FunctionDeclStmt.Parameter param = node.getParameters().get(i);
            builder.append(param.getName());

            if (param.getType() != null) {
                builder.append(": ").append(param.getType());
            }

            if (i < node.getParameters().size() - 1) {
                builder.append(", ");
            }
        }

        builder.append(")");

        if (node.getReturnType() != null) {
            builder.append(": ").append(node.getReturnType());
        }

        builder.append(" = ").append(node.getBody().accept(this));

        return builder.toString();
    }

    @Override
    public String visitAsyncFunctionDeclStmt(AsyncFunctionDeclStmt node) {
        return "async def";
    }

    @Override
    public String visitReturnStmt(ReturnStmt node) {
        return "return";
    }

    @Override
    public String visitTryCatchStmt(TryCatchStmt node) {
        return "try-catch";
    }

    @Override
    public String visitAwaitStmt(AwaitStmt node) {
        return "await";
    }

    @Override
    public String visitProgram(Program node) {
        StringBuilder builder = new StringBuilder();

        if (node.isStrictMode()) {
            builder.append("#!strict\n\n");
        }

        for (Stmt stmt : node.getStatements()) {
            builder.append(stmt.accept(this)).append("\n");
        }

        return builder.toString();
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }
}