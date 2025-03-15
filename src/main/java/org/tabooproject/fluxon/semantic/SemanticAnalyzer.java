package org.tabooproject.fluxon.semantic;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.expression.*;
import org.tabooproject.fluxon.ast.statement.*;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompilationPhase;

import java.util.ArrayList;
import java.util.List;

/**
 * 语义分析器
 * 用于分析 AST 的语义，包括符号解析、类型检查等
 */
public class SemanticAnalyzer implements CompilationPhase<Void>, AstVisitor<Void> {
    private final SymbolTable symbolTable = new SymbolTable();
    private final TypeInference typeInference = new TypeInference(symbolTable);
    private final TypeChecker typeChecker = new TypeChecker(symbolTable, typeInference);
    private final StrictModeChecker strictModeChecker;
    private final List<SemanticError> errors = new ArrayList<>();
    
    /**
     * 创建语义分析器
     */
    public SemanticAnalyzer() {
        // 解决循环依赖
        typeInference.setTypeChecker(typeChecker);
        strictModeChecker = new StrictModeChecker(false); // 默认不启用 Strict 模式
    }
    
    public SemanticAnalyzer(boolean strictMode) {
        typeInference.setTypeChecker(typeChecker);
        strictModeChecker = new StrictModeChecker(strictMode);
    }
    
    /**
     * 执行语义分析
     * 
     * @param context 编译上下文
     * @return null（语义分析不返回值）
     */
    @Override
    public Void process(CompilationContext context) {
        // 获取 AST
        Program program = context.getAttribute("ast");
        
        if (program == null) {
            throw new IllegalStateException("AST not found in compilation context");
        }
        
        // 分析程序
        program.accept(this);
        
        // 检查 Strict 模式
        // 修复 AST 中的字符串字面量
        fixLiteralExpressions(program);
        
        List<SemanticError> strictModeErrors = strictModeChecker.check(program);
        // 添加 Strict 模式错误
        errors.addAll(strictModeErrors);
        
        // 将错误添加到编译上下文
        context.setAttribute("semanticErrors", errors);
        
        // 将符号表添加到编译上下文
        context.setAttribute("symbolTable", symbolTable);
        
        return null;
    }
    
    @Override
    public Void visitProgram(Program node) {
        // 创建全局作用域
        symbolTable.enterScope();
        
        // 分析所有语句
        for (Stmt stmt : node.getStatements()) {
            stmt.accept(this);
        }
        
        // 退出全局作用域
        symbolTable.exitScope();
        
        return null;
    }
    
    @Override
    public Void visitVarDeclStmt(VarDeclStmt node) {
        // 检查变量声明
        typeChecker.checkVarDecl(node);
        
        // 如果有错误，不添加到符号表
        if (typeChecker.hasErrors()) {
            errors.addAll(typeChecker.getErrors());
            return null;
        }
        
        // 推断变量类型
        TypeInfo type;
        if (node.getTypeAnnotation() != null) {
            // 使用类型注解
            type = resolveType(node.getTypeAnnotation());
        } else if (node.getInitializer() != null) {
            // 使用初始化表达式的类型
            type = typeInference.inferType(node.getInitializer());
        } else {
            // 没有类型注解且没有初始化表达式，使用 Any 类型
            type = TypeInfo.ANY;
        }
        
        // 创建符号
        Symbol symbol = new Symbol(
                node.getName(),
                Symbol.SymbolKind.VARIABLE,
                type,
                !node.isVal()); // val 不可变，var 可变
        
        // 添加到符号表
        if (!symbolTable.define(node.getName(), symbol)) {
            errors.add(new SemanticError(
                    "Variable '" + node.getName() + "' is already defined in this scope",
                    node.getLocation()));
        }
        
        return null;
    }
    
    @Override
    public Void visitFunctionDeclStmt(FunctionDeclStmt node) {
        // 解析返回类型
        TypeInfo returnType = node.getReturnType() != null
                ? resolveType(node.getReturnType())
                : TypeInfo.ANY;
        
        // 解析参数类型
        List<TypeInfo> paramTypes = new ArrayList<>();
        for (FunctionDeclStmt.Parameter param : node.getParameters()) {
            TypeInfo paramType = param.getType() != null
                    ? resolveType(param.getType())
                    : TypeInfo.ANY;
            paramTypes.add(paramType);
        }
        
        // 创建函数类型
        TypeInfo functionType = TypeInfo.functionOf(paramTypes, returnType);
        
        // 创建符号
        Symbol symbol = new Symbol(
                node.getName(),
                Symbol.SymbolKind.FUNCTION,
                functionType,
                false); // 函数不可变
        
        // 添加到符号表
        if (!symbolTable.define(node.getName(), symbol)) {
            errors.add(new SemanticError(
                    "Function '" + node.getName() + "' is already defined in this scope",
                    node.getLocation()));
            return null;
        }
        
        // 创建新的作用域
        symbolTable.enterScope();
        
        // 添加参数到符号表
        for (int i = 0; i < node.getParameters().size(); i++) {
            FunctionDeclStmt.Parameter param = node.getParameters().get(i);
            TypeInfo paramType = paramTypes.get(i);
            
            Symbol paramSymbol = new Symbol(
                    param.getName(),
                    Symbol.SymbolKind.PARAMETER,
                    paramType,
                    false); // 参数不可变
            
            if (!symbolTable.define(param.getName(), paramSymbol)) {
                errors.add(new SemanticError(
                        "Parameter '" + param.getName() + "' is already defined",
                        node.getLocation()));
            }
        }
        
        // 分析函数体
        if (node.getBody() instanceof BlockExpr) {
            // 如果函数体是块表达式，分析块语句
            BlockStmt block = ((BlockExpr) node.getBody()).getBlock();
            visitBlockStmt(block);
        } else {
            // 否则，推断表达式类型
            TypeInfo bodyType = typeInference.inferType(node.getBody());
            
            // 检查返回类型兼容性
            if (!bodyType.isAssignableTo(returnType)) {
                errors.add(new SemanticError(
                        "Function body has type '" + bodyType + "' but return type is '" + returnType + "'",
                        node.getBody().getLocation()));
            }
        }
        
        // 退出作用域
        symbolTable.exitScope();
        
        return null;
    }
    
    @Override
    public Void visitAsyncFunctionDeclStmt(AsyncFunctionDeclStmt node) {
        // 解析返回类型
        TypeInfo returnType = node.getReturnType() != null
                ? resolveType(node.getReturnType())
                : TypeInfo.ANY;
        
        // 解析参数类型
        List<TypeInfo> paramTypes = new ArrayList<>();
        for (AsyncFunctionDeclStmt.Parameter param : node.getParameters()) {
            // 类似于 visitFunctionDeclStmt 的实现
        }
        
        return null;
    }
    
    @Override
    public Void visitBlockStmt(BlockStmt node) {
        // 创建新的作用域
        symbolTable.enterScope();
        
        // 分析所有语句
        for (Stmt stmt : node.getStatements()) {
            stmt.accept(this);
        }
        
        // 退出作用域
        symbolTable.exitScope();
        
        return null;
    }
    
    @Override
    public Void visitExpressionStmt(ExpressionStmt node) {
        // 推断表达式类型
        typeInference.inferType(node.getExpression());
        
        // 添加类型检查错误
        if (typeChecker.hasErrors()) {
            errors.addAll(typeChecker.getErrors());
        }
        
        return null;
    }
    
    @Override
    public Void visitReturnStmt(ReturnStmt node) {
        // 推断返回值类型
        if (node.getValue() != null) {
            typeInference.inferType(node.getValue());
            
            // 添加类型检查错误
            if (typeChecker.hasErrors()) {
                errors.addAll(typeChecker.getErrors());
            }
        }
        
        return null;
    }
    
    @Override
    public Void visitTryCatchStmt(TryCatchStmt node) {
        // 分析 try 块
        node.getTryBlock().accept(this);
        
        // 创建新的作用域
        symbolTable.enterScope();
        
        // 添加异常变量到符号表
        if (node.getExceptionVariable() != null) {
            TypeInfo exceptionType = node.getExceptionType() != null
                    ? resolveType(node.getExceptionType())
                    : TypeInfo.ANY;
            
            Symbol exceptionSymbol = new Symbol(
                    node.getExceptionVariable(),
                    Symbol.SymbolKind.VARIABLE,
                    exceptionType,
                    false); // 异常变量不可变
            
            symbolTable.define(node.getExceptionVariable(), exceptionSymbol);
        }
        
        // 分析 catch 块
        node.getCatchBlock().accept(this);
        
        // 退出作用域
        symbolTable.exitScope();
        
        return null;
    }
    
    @Override
    public Void visitAwaitStmt(AwaitStmt node) {
        // 推断表达式类型
        typeInference.inferType(node.getExpression());
        
        // 添加类型检查错误
        if (typeChecker.hasErrors()) {
            errors.addAll(typeChecker.getErrors());
        }
        
        return null;
    }
    
    @Override
    public Void visitBinaryExpr(BinaryExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    @Override
    public Void visitUnaryExpr(UnaryExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    @Override
    public Void visitLiteralExpr(LiteralExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    @Override
    public Void visitVariableExpr(VariableExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    @Override
    public Void visitCallExpr(CallExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    @Override
    public Void visitIfExpr(IfExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    @Override
    public Void visitWhenExpr(WhenExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    @Override
    public Void visitListExpr(ListExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    @Override
    public Void visitMapExpr(MapExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    @Override
    public Void visitRangeExpr(RangeExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    @Override
    public Void visitSafeAccessExpr(SafeAccessExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    @Override
    public Void visitElvisExpr(ElvisExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    @Override
    public Void visitLambdaExpr(LambdaExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    @Override
    public Void visitBlockExpr(BlockExpr node) {
        // 不需要实现，因为表达式通过 typeInference 处理
        return null;
    }
    
    /**
     * 获取语义错误
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
    
    /**
     * 解析类型名称
     * 
     * @param typeName 类型名称
     * @return 类型信息
     */
    private TypeInfo resolveType(String typeName) {
        // 处理基本类型
        switch (typeName) {
            case "Int":
                return TypeInfo.INT;
            case "Float":
                return TypeInfo.FLOAT;
            case "Boolean":
                return TypeInfo.BOOLEAN;
            case "String":
                return TypeInfo.STRING;
            case "Void":
                return TypeInfo.VOID;
            case "Any":
                return TypeInfo.ANY;
        }
        
        // TODO: 处理自定义类型和泛型类型
        
        return TypeInfo.UNKNOWN;
    }
    
    /**
     * 修复 AST 中的字符串字面量
     * 将可能是函数名或变量名的字符串字面量转换为相应的表达式类型
     * 
     * @param program AST 根节点
     */
    private void fixLiteralExpressions(Program program) {
        // 创建 AST 修复访问器
        AstFixVisitor visitor = new AstFixVisitor();
        // 修复 AST
        program.accept(visitor);
    }
    
    /**
     * AST 修复访问器
     * 用于修复 AST 中的字符串字面量
     */
    private class AstFixVisitor implements AstVisitor<Void> {

        @Override
        public Void visitCallExpr(CallExpr node) {
            // 如果函数名是字符串（来自字面量），检查它是否是已定义的函数
            if (node.getName() != null) {
                Symbol symbol = symbolTable.resolve(node.getName());
                if (symbol != null && symbol.getKind() == Symbol.SymbolKind.FUNCTION) {
                    // 函数名是已定义的函数，不需要修改
                    // 这里我们可以添加一些调试信息
                    System.out.println("Found function: " + node.getName());
                }
            }
            
            // 递归处理函数调用的参数
            // 修复函数调用中的参数
            for (int i = 0; i < node.getArguments().size(); i++) {
                Expr arg = node.getArguments().get(i);
                if (arg instanceof LiteralExpr && ((LiteralExpr) arg).getType() == LiteralExpr.LiteralType.STRING) {
                    // 获取字面量值
                    String value = (String) ((LiteralExpr) arg).getValue();
                    
                    // 检查是否是函数名
                    Symbol symbol = symbolTable.resolve(value);
                    if (symbol != null && symbol.getKind() == Symbol.SymbolKind.FUNCTION) {
                        // 将字符串字面量转换为变量引用
                        node.getArguments().set(i, new VariableExpr(value, arg.getLocation()));
                    }
                }
                
                // 递归修复参数中的表达式
                arg.accept(this);
            }
            
            return null;
        }
        
        // 实现其他访问方法，递归遍历 AST
        // 这里只列出了部分方法，实际实现需要完整遍历 AST
        
        @Override
        public Void visitProgram(Program node) {
            for (Stmt stmt : node.getStatements()) {
                stmt.accept(this);
            }
            return null;
        }
        
        @Override
        public Void visitExpressionStmt(ExpressionStmt node) {
            node.getExpression().accept(this);
            return null;
        }
        
        @Override
        public Void visitVarDeclStmt(VarDeclStmt node) {
            if (node.getInitializer() != null) {
                node.getInitializer().accept(this);
            }
            return null;
        }
        
        @Override
        public Void visitFunctionDeclStmt(FunctionDeclStmt node) {
            node.getBody().accept(this);
            return null;
        }

        @Override
        public Void visitAsyncFunctionDeclStmt(AsyncFunctionDeclStmt node) {
            return null;
        }

        @Override
        public Void visitBlockStmt(BlockStmt node) {
            for (Stmt stmt : node.getStatements()) {
                stmt.accept(this);
            }
            return null;
        }
        
        @Override
        public Void visitReturnStmt(ReturnStmt node) {
            if (node.getValue() != null) {
                node.getValue().accept(this);
            }
            return null;
        }
        
        @Override
        public Void visitTryCatchStmt(TryCatchStmt node) {
            node.getTryBlock().accept(this);
            node.getCatchBlock().accept(this);
            return null;
        }
        
        @Override
        public Void visitAwaitStmt(AwaitStmt node) {
            node.getExpression().accept(this);
            return null;
        }
        
        @Override
        public Void visitBinaryExpr(BinaryExpr node) {
            node.getLeft().accept(this);
            node.getRight().accept(this);
            return null;
        }
        
        @Override
        public Void visitUnaryExpr(UnaryExpr node) {
            node.getOperand().accept(this);
            return null;
        }
        
        @Override
        public Void visitLiteralExpr(LiteralExpr node) {
            // 字面量不需要递归处理
            return null;
        }
        
        @Override
        public Void visitVariableExpr(VariableExpr node) {
            // 变量引用不需要递归处理
            return null;
        }
        
        @Override
        public Void visitIfExpr(IfExpr node) {
            node.getCondition().accept(this);
            node.getThenBranch().accept(this);
            if (node.getElseBranch() != null) {
                node.getElseBranch().accept(this);
            }
            return null;
        }
        
        @Override
        public Void visitBlockExpr(BlockExpr node) {
            node.getBlock().accept(this);
            return null;
        }
        
        @Override
        public Void visitListExpr(ListExpr node) {
            for (Expr element : node.getElements()) {
                element.accept(this);
            }
            return null;
        }
        
        @Override
        public Void visitMapExpr(MapExpr node) {
            for (MapExpr.Entry entry : node.getEntries()) {
                entry.getKey().accept(this);
                entry.getValue().accept(this);
            }
            return null;
        }
        
        @Override
        public Void visitWhenExpr(WhenExpr node) {
            // 处理 when 的主体表达式
            if (node.getSubject() != null) {
                node.getSubject().accept(this);
            }
            
            // 处理所有分支
            for (WhenExpr.WhenBranch branch : node.getBranches()) {
                branch.getCondition().accept(this);
                branch.getBody().accept(this);
            }
            
            // 处理 else 分支
            if (node.getElseBranch() != null) {
                node.getElseBranch().accept(this);
            }
            return null;
        }
        
        @Override
        public Void visitRangeExpr(RangeExpr node) {
            node.getStart().accept(this);
            node.getEnd().accept(this);
            return null;
        }
        
        @Override
        public Void visitSafeAccessExpr(SafeAccessExpr node) {
            node.getObject().accept(this);
            return null;
        }
        
        @Override
        public Void visitElvisExpr(ElvisExpr node) {
            node.getCondition().accept(this);
            node.getFallback().accept(this);
            return null;
        }
        
        @Override
        public Void visitLambdaExpr(LambdaExpr node) {
            node.getBody().accept(this);
            return null;
        }
    }
}