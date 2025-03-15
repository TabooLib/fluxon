package org.tabooproject.fluxon.ir.instruction;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRValue;
import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.FunctionType;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数调用指令
 * 调用函数
 */
public class CallInst extends AbstractInstruction {
    private final IRValue function; // 函数
    private final List<IRValue> arguments; // 参数
    
    /**
     * 创建函数调用指令
     * 
     * @param function 函数
     * @param arguments 参数
     * @param name 指令名称
     */
    public CallInst(IRValue function, List<IRValue> arguments, String name) {
        super(getReturnType(function), name);
        this.function = function;
        this.arguments = new ArrayList<>(arguments);
        
        // 检查参数数量
        FunctionType functionType = getFunctionType(function);
        if (functionType.getParamTypes().length != arguments.size()) {
            throw new IllegalArgumentException("Function expects " + functionType.getParamTypes().length +
                    " arguments, but got " + arguments.size());
        }
        
        // 检查参数类型
        for (int i = 0; i < arguments.size(); i++) {
            IRType paramType = functionType.getParamTypes()[i];
            IRType argType = arguments.get(i).getType();
            
            if (!argType.equals(paramType)) {
                throw new IllegalArgumentException("Argument " + i + " type mismatch: expected " +
                        paramType + ", but got " + argType);
            }
        }
    }
    
    /**
     * 获取函数
     * 
     * @return 函数
     */
    public IRValue getFunction() {
        return function;
    }
    
    /**
     * 获取参数
     * 
     * @return 参数
     */
    public List<IRValue> getArguments() {
        return arguments;
    }
    
    @Override
    public boolean hasSideEffects() {
        return true;
    }
    
    /**
     * 获取函数类型
     * 
     * @param function 函数
     * @return 函数类型
     */
    private static FunctionType getFunctionType(IRValue function) {
        IRType type = function.getType();
        if (!(type instanceof FunctionType)) {
            throw new IllegalArgumentException("Expected function type, got " + type);
        }
        
        return (FunctionType) type;
    }
    
    /**
     * 获取返回类型
     * 
     * @param function 函数
     * @return 返回类型
     */
    private static IRType getReturnType(IRValue function) {
        return getFunctionType(function).getReturnType();
    }
    
    @Override
    protected String getInstructionName() {
        return "call";
    }
    
    @Override
    protected String getOperandsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(function).append("(");
        
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(arguments.get(i));
        }
        
        sb.append(")");
        return sb.toString();
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitCallInst(this);
    }
}