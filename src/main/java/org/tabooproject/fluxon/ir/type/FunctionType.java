package org.tabooproject.fluxon.ir.type;

import org.tabooproject.fluxon.ir.IRType;

import java.util.Arrays;

/**
 * 函数类型
 * 表示函数的类型
 */
public class FunctionType extends AbstractIRType {
    private final IRType returnType;
    private final IRType[] paramTypes;
    
    /**
     * 创建函数类型
     * 
     * @param returnType 返回类型
     * @param paramTypes 参数类型
     */
    public FunctionType(IRType returnType, IRType... paramTypes) {
        super(buildName(returnType, paramTypes));
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }
    
    /**
     * 获取返回类型
     * 
     * @return 返回类型
     */
    public IRType getReturnType() {
        return returnType;
    }
    
    /**
     * 获取参数类型
     * 
     * @return 参数类型
     */
    public IRType[] getParamTypes() {
        return paramTypes;
    }
    
    @Override
    public int getSize() {
        return 8; // 函数指针大小为 8 字节（64 位）
    }
    
    @Override
    public boolean isFunctionType() {
        return true;
    }
    
    /**
     * 构建函数类型名称
     * 
     * @param returnType 返回类型
     * @param paramTypes 参数类型
     * @return 函数类型名称
     */
    private static String buildName(IRType returnType, IRType... paramTypes) {
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(returnType.getName()).append(" (");
        
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) {
                nameBuilder.append(", ");
            }
            nameBuilder.append(paramTypes[i].getName());
        }
        
        nameBuilder.append(")");
        return nameBuilder.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        FunctionType that = (FunctionType) o;
        
        if (!returnType.equals(that.returnType)) return false;
        return Arrays.equals(paramTypes, that.paramTypes);
    }
    
    @Override
    public int hashCode() {
        int result = returnType.hashCode();
        result = 31 * result + Arrays.hashCode(paramTypes);
        return result;
    }
}