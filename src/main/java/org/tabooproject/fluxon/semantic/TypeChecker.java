package org.tabooproject.fluxon.semantic;

import org.tabooproject.fluxon.ast.expression.*;
import org.tabooproject.fluxon.ast.statement.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 类型检查器
 * 用于检查表达式和语句的类型正确性
 */
public class TypeChecker {
    private final SymbolTable symbolTable;
    private final TypeInference typeInference;
    private final List<SemanticError> errors = new ArrayList<>();
    
    /**
     * 创建类型检查器
     * 
     * @param symbolTable 符号表
     * @param typeInference 类型推断器
     */
    public TypeChecker(SymbolTable symbolTable, TypeInference typeInference) {
        this.symbolTable = symbolTable;
        this.typeInference = typeInference;
    }
    
    /**
     * 检查变量声明
     * 
     * @param stmt 变量声明语句
     */
    public void checkVarDecl(VarDeclStmt stmt) {
        // 检查变量是否已定义
        if (symbolTable.isDefined(stmt.getName())) {
            errors.add(new SemanticError(
                    "Variable '" + stmt.getName() + "' is already defined in this scope",
                    stmt.getLocation()));
            return;
        }
        
        // 检查初始化表达式
        if (stmt.getInitializer() != null) {
            TypeInfo initializerType = typeInference.inferType(stmt.getInitializer());
            
            // 如果有类型注解，检查类型兼容性
            if (stmt.getTypeAnnotation() != null) {
                TypeInfo annotationType = resolveType(stmt.getTypeAnnotation());
                
                if (!initializerType.isAssignableTo(annotationType)) {
                    errors.add(new SemanticError(
                            "Cannot assign value of type '" + initializerType + "' to variable of type '" + annotationType + "'",
                            stmt.getLocation()));
                }
            }
        } else if (stmt.getTypeAnnotation() == null) {
            // 如果没有初始化表达式且没有类型注解，则无法确定类型
            errors.add(new SemanticError(
                    "Variable '" + stmt.getName() + "' must either have a type annotation or an initializer",
                    stmt.getLocation()));
        }
    }
    
    /**
     * 检查函数声明
     * 
     * @param stmt 函数声明语句
     */
    public void checkFunctionDecl(FunctionDeclStmt stmt) {
        // 检查函数是否已定义
        if (symbolTable.isDefined(stmt.getName())) {
            errors.add(new SemanticError(
                    "Function '" + stmt.getName() + "' is already defined in this scope",
                    stmt.getLocation()));
            return;
        }
        
        // 创建新的作用域
        symbolTable.enterScope();
        
        // 检查参数
        for (FunctionDeclStmt.Parameter param : stmt.getParameters()) {
            // 检查参数是否已定义
            if (symbolTable.isDefined(param.getName())) {
                errors.add(new SemanticError(
                        "Parameter '" + param.getName() + "' is already defined",
                        stmt.getLocation()));
                continue;
            }
            
            // 解析参数类型
            TypeInfo paramType = param.getType() != null
                    ? resolveType(param.getType())
                    : TypeInfo.ANY;
            
            // 将参数添加到符号表
            Symbol paramSymbol = new Symbol(
                    param.getName(),
                    Symbol.SymbolKind.PARAMETER,
                    paramType,
                    false); // 参数不可变
            
            symbolTable.define(param.getName(), paramSymbol);
        }
        
        // 解析返回类型
        TypeInfo returnType = stmt.getReturnType() != null
                ? resolveType(stmt.getReturnType())
                : TypeInfo.ANY;
        
        // 检查函数体
        TypeInfo bodyType = typeInference.inferType(stmt.getBody());
        
        if (!bodyType.isAssignableTo(returnType)) {
            errors.add(new SemanticError(
                    "Function body has type '" + bodyType + "' but return type is '" + returnType + "'",
                    stmt.getLocation()));
        }
        
        // 退出作用域
        symbolTable.exitScope();
    }
    
    /**
     * 检查表达式语句
     * 
     * @param stmt 表达式语句
     */
    public void checkExpressionStmt(ExpressionStmt stmt) {
        // 只需要推断表达式的类型，不需要进行特殊检查
        typeInference.inferType(stmt.getExpression());
    }
    
    /**
     * 检查二元表达式
     * 
     * @param expr 二元表达式
     * @return 表达式类型
     */
    public TypeInfo checkBinaryExpr(BinaryExpr expr) {
        TypeInfo leftType = typeInference.inferType(expr.getLeft());
        TypeInfo rightType = typeInference.inferType(expr.getRight());
        
        switch (expr.getOperator()) {
            case ADD:
            case SUBTRACT:
            case MULTIPLY:
            case DIVIDE:
            case MODULO:
                // 算术运算符要求操作数为数值类型
                if (!isNumericType(leftType) || !isNumericType(rightType)) {
                    errors.add(new SemanticError(
                            "Operator '" + expr.getOperator().getSymbol() + "' cannot be applied to types '" + leftType + "' and '" + rightType + "'",
                            expr.getLocation()));
                    return TypeInfo.UNKNOWN;
                }
                
                // 如果有一个操作数是浮点数，结果就是浮点数
                if (leftType.equals(TypeInfo.FLOAT) || rightType.equals(TypeInfo.FLOAT)) {
                    return TypeInfo.FLOAT;
                }
                
                return TypeInfo.INT;
                
            case EQUAL:
            case NOT_EQUAL:
                // 相等性运算符可以应用于任何类型，但两个操作数类型应该兼容
                if (!leftType.isAssignableTo(rightType) && !rightType.isAssignableTo(leftType)) {
                    errors.add(new SemanticError(
                            "Incompatible types '" + leftType + "' and '" + rightType + "' for equality comparison",
                            expr.getLocation()));
                }
                return TypeInfo.BOOLEAN;
                
            case GREATER:
            case LESS:
            case GREATER_EQUAL:
            case LESS_EQUAL:
                // 比较运算符要求操作数为数值类型或字符串类型
                if ((!isNumericType(leftType) && !leftType.equals(TypeInfo.STRING)) ||
                        (!isNumericType(rightType) && !rightType.equals(TypeInfo.STRING))) {
                    errors.add(new SemanticError(
                            "Operator '" + expr.getOperator().getSymbol() + "' cannot be applied to types '" + leftType + "' and '" + rightType + "'",
                            expr.getLocation()));
                }
                return TypeInfo.BOOLEAN;
                
            case AND:
            case OR:
                // 逻辑运算符要求操作数为布尔类型
                if (!leftType.equals(TypeInfo.BOOLEAN) || !rightType.equals(TypeInfo.BOOLEAN)) {
                    errors.add(new SemanticError(
                            "Operator '" + expr.getOperator().getSymbol() + "' cannot be applied to types '" + leftType + "' and '" + rightType + "'",
                            expr.getLocation()));
                }
                return TypeInfo.BOOLEAN;
                
            default:
                return TypeInfo.UNKNOWN;
        }
    }
    
    /**
     * 检查一元表达式
     * 
     * @param expr 一元表达式
     * @return 表达式类型
     */
    public TypeInfo checkUnaryExpr(UnaryExpr expr) {
        TypeInfo operandType = typeInference.inferType(expr.getOperand());
        
        switch (expr.getOperator()) {
            case NEGATE:
                // 负号运算符要求操作数为数值类型
                if (!isNumericType(operandType)) {
                    errors.add(new SemanticError(
                            "Operator '-' cannot be applied to type '" + operandType + "'",
                            expr.getLocation()));
                    return TypeInfo.UNKNOWN;
                }
                return operandType;
                
            case NOT:
                // 逻辑非运算符要求操作数为布尔类型
                if (!operandType.equals(TypeInfo.BOOLEAN)) {
                    errors.add(new SemanticError(
                            "Operator '!' cannot be applied to type '" + operandType + "'",
                            expr.getLocation()));
                    return TypeInfo.UNKNOWN;
                }
                return TypeInfo.BOOLEAN;
                
            default:
                return TypeInfo.UNKNOWN;
        }
    }
    
    /**
     * 检查变量引用表达式
     * 
     * @param expr 变量引用表达式
     * @return 表达式类型
     */
    public TypeInfo checkVariableExpr(VariableExpr expr) {
        Symbol symbol = symbolTable.resolve(expr.getName());
        
        if (symbol == null) {
            errors.add(new SemanticError(
                    "Variable '" + expr.getName() + "' is not defined",
                    expr.getLocation()));
            return TypeInfo.UNKNOWN;
        }
        
        return symbol.getType();
    }
    
    /**
     * 检查函数调用表达式
     * 
     * @param expr 函数调用表达式
     * @return 表达式类型
     */
    public TypeInfo checkCallExpr(CallExpr expr) {
        Symbol symbol = symbolTable.resolve(expr.getName());
        
        if (symbol == null) {
            errors.add(new SemanticError(
                    "Function '" + expr.getName() + "' is not defined",
                    expr.getLocation()));
            return TypeInfo.UNKNOWN;
        }
        
        if (symbol.getKind() != Symbol.SymbolKind.FUNCTION) {
            errors.add(new SemanticError(
                    "'" + expr.getName() + "' is not a function",
                    expr.getLocation()));
            return TypeInfo.UNKNOWN;
        }
        
        TypeInfo functionType = symbol.getType();
        
        if (functionType.getKind() != TypeInfo.TypeKind.FUNCTION) {
            errors.add(new SemanticError(
                    "'" + expr.getName() + "' is not a function",
                    expr.getLocation()));
            return TypeInfo.UNKNOWN;
        }
        
        List<TypeInfo> paramTypes = new ArrayList<>(functionType.getTypeParameters());
        // 最后一个类型参数是返回类型
        TypeInfo returnType = paramTypes.remove(paramTypes.size() - 1);
        
        // 检查参数数量
        if (expr.getArguments().size() != paramTypes.size()) {
            errors.add(new SemanticError(
                    "Function '" + expr.getName() + "' expects " + paramTypes.size() + " arguments, but got " + expr.getArguments().size(),
                    expr.getLocation()));
            return returnType;
        }
        
        // 检查参数类型
        for (int i = 0; i < expr.getArguments().size(); i++) {
            TypeInfo argType = typeInference.inferType(expr.getArguments().get(i));
            TypeInfo paramType = paramTypes.get(i);
            
            if (!argType.isAssignableTo(paramType)) {
                errors.add(new SemanticError(
                        "Argument " + (i + 1) + " has type '" + argType + "' but parameter expects type '" + paramType + "'",
                        expr.getArguments().get(i).getLocation()));
            }
        }
        
        return returnType;
    }
    
    /**
     * 获取类型检查错误
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
     * 检查是否为数值类型
     * 
     * @param type 类型信息
     * @return 是否为数值类型
     */
    private boolean isNumericType(TypeInfo type) {
        return type.equals(TypeInfo.INT) || type.equals(TypeInfo.FLOAT);
    }
}