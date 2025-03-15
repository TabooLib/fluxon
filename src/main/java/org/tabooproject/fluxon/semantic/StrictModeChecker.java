package org.tabooproject.fluxon.semantic;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.expression.*;
import org.tabooproject.fluxon.ast.statement.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Strict 模式检查器
 * 用于检查代码是否符合 Strict 模式的要求
 */
public class StrictModeChecker implements AstVisitor<Void> {
    private final List<SemanticError> errors = new ArrayList<>();
    private final boolean strictMode;
    
    /**
     * 创建 Strict 模式检查器
     * 
     * @param strictMode 是否启用 Strict 模式
     */
    public StrictModeChecker(boolean strictMode) {
        this.strictMode = strictMode;
    }
    
    /**
     * 检查程序是否符合 Strict 模式的要求
     * 
     * @param program 程序
     * @return 错误列表
     */
    public List<SemanticError> check(Program program) {
        program.accept(this);
        return errors;
    }
    
    @Override
    public Void visitProgram(Program node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查程序中的所有语句
        for (Stmt stmt : node.getStatements()) {
            stmt.accept(this);
        }
        
        return null;
    }
    
    @Override
    public Void visitFunctionDeclStmt(FunctionDeclStmt node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查函数返回类型
        if (node.getReturnType() == null) {
            errors.add(new SemanticError(
                    "Function '" + node.getName() + "' must have an explicit return type in strict mode",
                    node.getLocation()));
        }
        
        // 检查函数参数类型
        for (FunctionDeclStmt.Parameter param : node.getParameters()) {
            if (param.getType() == null) {
                errors.add(new SemanticError(
                        "Parameter '" + param.getName() + "' must have an explicit type in strict mode",
                        node.getLocation()));
            }
        }
        
        // 检查函数体
        node.getBody().accept(this);
        
        return null;
    }
    
    @Override
    public Void visitAsyncFunctionDeclStmt(AsyncFunctionDeclStmt node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查函数返回类型
        if (node.getReturnType() == null) {
            errors.add(new SemanticError(
                    "Async function '" + node.getName() + "' must have an explicit return type in strict mode",
                    node.getLocation()));
        }
        
        // 检查函数参数类型
        for (AsyncFunctionDeclStmt.Parameter param : node.getParameters()) {
            if (param.getType() == null) {
                errors.add(new SemanticError(
                        "Parameter '" + param.getName() + "' must have an explicit type in strict mode",
                        node.getLocation()));
            }
        }
        
        // 检查函数体
        node.getBody().accept(this);
        
        return null;
    }
    
    @Override
    public Void visitVarDeclStmt(VarDeclStmt node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查变量类型
        if (node.getTypeAnnotation() == null) {
            errors.add(new SemanticError(
                    "Variable '" + node.getName() + "' must have an explicit type in strict mode",
                    node.getLocation()));
        }
        
        // 检查初始化表达式
        if (node.getInitializer() != null) {
            node.getInitializer().accept(this);
        }
        
        return null;
    }
    
    @Override
    public Void visitCallExpr(CallExpr node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查函数调用参数
        for (Expr arg : node.getArguments()) {
            // 禁止隐式字符串转换
            if (arg instanceof LiteralExpr && ((LiteralExpr) arg).getType() == LiteralExpr.LiteralType.IDENTIFIER) {
                errors.add(new SemanticError(
                        "Implicit string conversion is not allowed in strict mode. Use explicit string literals.",
                        arg.getLocation()));
            }
            
            arg.accept(this);
        }
        
        return null;
    }
    
    @Override
    public Void visitBlockStmt(BlockStmt node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查块中的所有语句
        for (Stmt stmt : node.getStatements()) {
            stmt.accept(this);
        }
        
        return null;
    }
    
    @Override
    public Void visitExpressionStmt(ExpressionStmt node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查表达式
        node.getExpression().accept(this);
        
        return null;
    }
    
    @Override
    public Void visitReturnStmt(ReturnStmt node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查返回值
        if (node.getValue() != null) {
            node.getValue().accept(this);
        }
        
        return null;
    }
    
    @Override
    public Void visitTryCatchStmt(TryCatchStmt node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查 try 块
        node.getTryBlock().accept(this);
        
        // 检查 catch 块
        node.getCatchBlock().accept(this);
        
        return null;
    }
    
    @Override
    public Void visitAwaitStmt(AwaitStmt node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查等待的表达式
        node.getExpression().accept(this);
        
        return null;
    }
    
    @Override
    public Void visitBinaryExpr(BinaryExpr node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查左右操作数
        node.getLeft().accept(this);
        node.getRight().accept(this);
        
        return null;
    }
    
    @Override
    public Void visitUnaryExpr(UnaryExpr node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查操作数
        node.getOperand().accept(this);
        
        return null;
    }
    
    @Override
    public Void visitLiteralExpr(LiteralExpr node) {
        // 字面量不需要检查
        return null;
    }
    
    @Override
    public Void visitVariableExpr(VariableExpr node) {
        // 变量引用不需要检查
        return null;
    }
    
    @Override
    public Void visitIfExpr(IfExpr node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查条件
        node.getCondition().accept(this);
        
        // 检查分支
        node.getThenBranch().accept(this);
        if (node.getElseBranch() != null) {
            node.getElseBranch().accept(this);
        }
        
        return null;
    }
    
    @Override
    public Void visitWhenExpr(WhenExpr node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查条件
        if (node.getSubject() != null) {
            node.getSubject().accept(this);
        }
        
        // 检查分支
        for (WhenExpr.WhenBranch branch : node.getBranches()) {
            branch.getCondition().accept(this);
            branch.getBody().accept(this);
        }
        
        // 检查 else 分支
        if (node.getElseBranch() != null) {
            node.getElseBranch().accept(this);
        }
        
        return null;
    }
    
    @Override
    public Void visitListExpr(ListExpr node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查列表元素
        for (Expr element : node.getElements()) {
            element.accept(this);
        }
        
        return null;
    }
    
    @Override
    public Void visitMapExpr(MapExpr node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查字典键值对
        for (MapExpr.Entry entry : node.getEntries()) {
            entry.getKey().accept(this);
            entry.getValue().accept(this);
        }
        
        return null;
    }
    
    @Override
    public Void visitRangeExpr(RangeExpr node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查范围起始和结束
        node.getStart().accept(this);
        node.getEnd().accept(this);
        
        return null;
    }
    
    @Override
    public Void visitSafeAccessExpr(SafeAccessExpr node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查对象
        node.getObject().accept(this);
        
        return null;
    }
    
    @Override
    public Void visitElvisExpr(ElvisExpr node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查条件和备选
        node.getCondition().accept(this);
        node.getFallback().accept(this);
        
        return null;
    }
    
    @Override
    public Void visitLambdaExpr(LambdaExpr node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查参数类型
        for (LambdaExpr.Parameter param : node.getParameters()) {
            if (param.getType() == null) {
                errors.add(new SemanticError(
                        "Lambda parameter '" + param.getName() + "' must have an explicit type in strict mode",
                        node.getLocation()));
            }
        }
        
        // 检查函数体
        node.getBody().accept(this);
        
        return null;
    }
    
    @Override
    public Void visitBlockExpr(BlockExpr node) {
        if (!strictMode) {
            return null;
        }
        
        // 检查块
        node.getBlock().accept(this);
        
        return null;
    }
    
    /**
     * 获取错误列表
     * 
     * @return 错误列表
     */
    public List<SemanticError> getErrors() {
        return errors;
    }
    
    /**
     * 检查是否有错误
     * 
     * @return 是否有错误
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}