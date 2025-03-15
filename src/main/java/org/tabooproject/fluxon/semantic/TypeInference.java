package org.tabooproject.fluxon.semantic;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.expression.*;
import org.tabooproject.fluxon.ast.statement.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类型推断器
 * 用于推断表达式和语句的类型
 */
public class TypeInference implements AstVisitor<TypeInfo> {
    private final SymbolTable symbolTable;
    private TypeChecker typeChecker;
    private final Map<Expr, TypeInfo> exprTypeCache = new HashMap<>();
    
    /**
     * 创建类型推断器
     * 
     * @param symbolTable 符号表
     */
    public TypeInference(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.typeChecker = null; // 将在 setTypeChecker 中设置
    }
    
    /**
     * 设置类型检查器
     * 
     * @param typeChecker 类型检查器
     */
    public void setTypeChecker(TypeChecker typeChecker) {
        // 使用这种方式解决循环依赖
        if (this.typeChecker == null) {
            // 只允许设置一次
            this.typeChecker = typeChecker;
        }
    }
    
    /**
     * 推断表达式的类型
     * 
     * @param expr 表达式
     * @return 类型信息
     */
    public TypeInfo inferType(Expr expr) {
        // 检查缓存
        if (exprTypeCache.containsKey(expr)) {
            return exprTypeCache.get(expr);
        }
        
        // 推断类型
        TypeInfo type = expr.accept(this);
        
        // 缓存结果
        exprTypeCache.put(expr, type);
        
        return type;
    }
    
    @Override
    public TypeInfo visitBinaryExpr(BinaryExpr node) {
        // 使用类型检查器检查二元表达式
        return typeChecker.checkBinaryExpr(node);
    }
    
    @Override
    public TypeInfo visitUnaryExpr(UnaryExpr node) {
        // 使用类型检查器检查一元表达式
        return typeChecker.checkUnaryExpr(node);
    }
    
    @Override
    public TypeInfo visitLiteralExpr(LiteralExpr node) {
        // 根据字面量类型确定表达式类型
        switch (node.getType()) {
            case INTEGER:
                return TypeInfo.INT;
            case FLOAT:
                return TypeInfo.FLOAT;
            case BOOLEAN:
                return TypeInfo.BOOLEAN;
            case STRING:
                return TypeInfo.STRING;
            default:
                return TypeInfo.UNKNOWN;
        }
    }
    
    @Override
    public TypeInfo visitVariableExpr(VariableExpr node) {
        // 使用类型检查器检查变量引用
        return typeChecker.checkVariableExpr(node);
    }
    
    @Override
    public TypeInfo visitCallExpr(CallExpr node) {
        // 使用类型检查器检查函数调用
        return typeChecker.checkCallExpr(node);
    }
    
    @Override
    public TypeInfo visitIfExpr(IfExpr node) {
        // 检查条件表达式
        TypeInfo conditionType = inferType(node.getCondition());
        if (!conditionType.equals(TypeInfo.BOOLEAN)) {
            // 条件表达式必须是布尔类型
            // 这里不添加错误，因为类型检查器会处理
        }
        
        // 推断分支表达式的类型
        TypeInfo thenType = inferType(node.getThenBranch());
        
        if (node.getElseBranch() != null) {
            TypeInfo elseType = inferType(node.getElseBranch());
            
            // if-else 表达式的类型是两个分支类型的最小公共超类型
            // 这里简化处理，如果两个类型不同，则返回 Any 类型
            if (thenType.equals(elseType)) {
                return thenType;
            } else {
                return TypeInfo.ANY;
            }
        } else {
            // 如果没有 else 分支，则类型为 then 分支的类型
            return thenType;
        }
    }
    
    @Override
    public TypeInfo visitWhenExpr(WhenExpr node) {
        // 暂时返回 Any 类型，后续实现
        return TypeInfo.ANY;
    }
    
    @Override
    public TypeInfo visitListExpr(ListExpr node) {
        // 如果列表为空，则返回 List<Any> 类型
        if (node.getElements().isEmpty()) {
            return TypeInfo.listOf(TypeInfo.ANY);
        }
        
        // 推断元素类型
        List<TypeInfo> elementTypes = new ArrayList<>();
        for (Expr element : node.getElements()) {
            elementTypes.add(inferType(element));
        }
        
        // 找出元素类型的最小公共超类型
        // 这里简化处理，如果所有元素类型相同，则使用该类型；否则使用 Any 类型
        TypeInfo elementType = elementTypes.get(0);
        for (int i = 1; i < elementTypes.size(); i++) {
            if (!elementType.equals(elementTypes.get(i))) {
                elementType = TypeInfo.ANY;
                break;
            }
        }
        
        return TypeInfo.listOf(elementType);
    }
    
    @Override
    public TypeInfo visitMapExpr(MapExpr node) {
        // 如果映射为空，则返回 Map<Any, Any> 类型
        if (node.getEntries().isEmpty()) {
            return TypeInfo.mapOf(TypeInfo.ANY, TypeInfo.ANY);
        }
        
        // 推断键和值的类型
        List<TypeInfo> keyTypes = new ArrayList<>();
        List<TypeInfo> valueTypes = new ArrayList<>();
        
        for (MapExpr.Entry entry : node.getEntries()) {
            keyTypes.add(inferType(entry.getKey()));
            valueTypes.add(inferType(entry.getValue()));
        }
        
        // 找出键和值类型的最小公共超类型
        // 这里简化处理，如果所有键/值类型相同，则使用该类型；否则使用 Any 类型
        TypeInfo keyType = keyTypes.get(0);
        for (int i = 1; i < keyTypes.size(); i++) {
            if (!keyType.equals(keyTypes.get(i))) {
                keyType = TypeInfo.ANY;
                break;
            }
        }
        
        TypeInfo valueType = valueTypes.get(0);
        for (int i = 1; i < valueTypes.size(); i++) {
            if (!valueType.equals(valueTypes.get(i))) {
                valueType = TypeInfo.ANY;
                break;
            }
        }
        
        return TypeInfo.mapOf(keyType, valueType);
    }
    
    @Override
    public TypeInfo visitRangeExpr(RangeExpr node) {
        // 检查起始和结束表达式的类型
        TypeInfo startType = inferType(node.getStart());
        TypeInfo endType = inferType(node.getEnd());
        
        // 范围表达式的操作数必须是整数类型
        if (!startType.equals(TypeInfo.INT) || !endType.equals(TypeInfo.INT)) {
            // 这里不添加错误，因为类型检查器会处理
        }
        
        // 范围表达式的类型是 List<Int>
        return TypeInfo.listOf(TypeInfo.INT);
    }
    
    @Override
    public TypeInfo visitSafeAccessExpr(SafeAccessExpr node) {
        // 暂时返回 Any 类型，后续实现
        return TypeInfo.ANY;
    }
    
    @Override
    public TypeInfo visitElvisExpr(ElvisExpr node) {
        TypeInfo conditionType = inferType(node.getCondition());
        TypeInfo fallbackType = inferType(node.getFallback());
        
        // Elvis 表达式的类型是两个操作数类型的最小公共超类型
        // 这里简化处理，如果两个类型不同，则返回 Any 类型
        if (conditionType.equals(fallbackType)) {
            return conditionType;
        } else {
            return TypeInfo.ANY;
        }
    }
    
    @Override
    public TypeInfo visitLambdaExpr(LambdaExpr node) {
        // 创建新的作用域
        symbolTable.enterScope();
        
        // 处理参数
        List<TypeInfo> paramTypes = new ArrayList<>();
        for (LambdaExpr.Parameter param : node.getParameters()) {
            // 解析参数类型
            TypeInfo paramType = param.getType() != null
                    ? resolveType(param.getType())
                    : TypeInfo.ANY;
            
            paramTypes.add(paramType);
            
            // 将参数添加到符号表
            Symbol paramSymbol = new Symbol(
                    param.getName(),
                    Symbol.SymbolKind.PARAMETER,
                    paramType,
                    false); // 参数不可变
            
            symbolTable.define(param.getName(), paramSymbol);
        }
        
        // 推断函数体类型
        TypeInfo bodyType = inferType(node.getBody());
        
        // 退出作用域
        symbolTable.exitScope();
        
        // 创建函数类型
        return TypeInfo.functionOf(paramTypes, bodyType);
    }
    
    @Override
    public TypeInfo visitBlockExpr(BlockExpr node) {
        // 创建新的作用域
        symbolTable.enterScope();
        
        // 处理块中的语句
        List<Stmt> statements = node.getBlock().getStatements();
        
        // 块表达式的类型是最后一个语句的类型
        // 如果块为空，则类型为 Void
        TypeInfo blockType = TypeInfo.VOID;
        
        for (Stmt stmt : statements) {
            if (stmt instanceof ExpressionStmt) {
                // 表达式语句的类型是表达式的类型
                blockType = inferType(((ExpressionStmt) stmt).getExpression());
            } else {
                // 其他语句的类型是 Void
                blockType = TypeInfo.VOID;
            }
        }
        
        // 退出作用域
        symbolTable.exitScope();
        
        return blockType;
    }
    
    @Override
    public TypeInfo visitBlockStmt(BlockStmt node) {
        // 块语句没有类型
        return TypeInfo.VOID;
    }
    
    @Override
    public TypeInfo visitExpressionStmt(ExpressionStmt node) {
        // 表达式语句的类型是表达式的类型
        return inferType(node.getExpression());
    }
    
    @Override
    public TypeInfo visitVarDeclStmt(VarDeclStmt node) {
        // 变量声明语句没有类型
        return TypeInfo.VOID;
    }
    
    @Override
    public TypeInfo visitFunctionDeclStmt(FunctionDeclStmt node) {
        // 函数声明语句没有类型
        return TypeInfo.VOID;
    }
    
    @Override
    public TypeInfo visitAsyncFunctionDeclStmt(AsyncFunctionDeclStmt node) {
        // 异步函数声明语句没有类型
        return TypeInfo.VOID;
    }
    
    @Override
    public TypeInfo visitReturnStmt(ReturnStmt node) {
        // 返回语句的类型是返回值的类型
        if (node.getValue() != null) {
            return inferType(node.getValue());
        } else {
            return TypeInfo.VOID;
        }
    }
    
    @Override
    public TypeInfo visitTryCatchStmt(TryCatchStmt node) {
        // try-catch 语句没有类型
        return TypeInfo.VOID;
    }
    
    @Override
    public TypeInfo visitAwaitStmt(AwaitStmt node) {
        // await 语句的类型是表达式的类型
        return inferType(node.getExpression());
    }
    
    @Override
    public TypeInfo visitProgram(Program node) {
        // 程序没有类型
        return TypeInfo.VOID;
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
}