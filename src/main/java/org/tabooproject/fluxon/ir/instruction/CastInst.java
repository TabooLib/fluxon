package org.tabooproject.fluxon.ir.instruction;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRValue;
import org.tabooproject.fluxon.ir.IRVisitor;

/**
 * 类型转换指令
 * 将值从一种类型转换为另一种类型
 */
public class CastInst extends AbstractInstruction {
    /**
     * 转换类型
     */
    public enum CastType {
        /**
         * 整数扩展（有符号）
         */
        SEXT("sext"),
        
        /**
         * 整数扩展（无符号）
         */
        ZEXT("zext"),
        
        /**
         * 整数截断
         */
        TRUNC("trunc"),
        
        /**
         * 浮点数扩展
         */
        FPEXT("fpext"),
        
        /**
         * 浮点数截断
         */
        FPTRUNC("fptrunc"),
        
        /**
         * 整数转浮点数
         */
        SITOFP("sitofp"),
        
        /**
         * 浮点数转整数
         */
        FPTOSI("fptosi"),
        
        /**
         * 指针转整数
         */
        PTRTOINT("ptrtoint"),
        
        /**
         * 整数转指针
         */
        INTTOPTR("inttoptr"),
        
        /**
         * 指针类型转换
         */
        BITCAST("bitcast");
        
        private final String symbol;
        
        CastType(String symbol) {
            this.symbol = symbol;
        }
        
        /**
         * 获取转换类型符号
         * 
         * @return 转换类型符号
         */
        public String getSymbol() {
            return symbol;
        }
    }
    
    private final CastType castType; // 转换类型
    private final IRValue value; // 要转换的值
    private final IRType sourceType; // 源类型
    
    /**
     * 创建类型转换指令
     * 
     * @param castType 转换类型
     * @param value 要转换的值
     * @param targetType 目标类型
     * @param name 指令名称
     */
    public CastInst(CastType castType, IRValue value, IRType targetType, String name) {
        super(targetType, name);
        this.castType = castType;
        this.value = value;
        this.sourceType = value.getType();
        
        // 检查类型兼容性
        checkTypeCompatibility(castType, sourceType, targetType);
    }
    
    /**
     * 获取转换类型
     * 
     * @return 转换类型
     */
    public CastType getCastType() {
        return castType;
    }
    
    /**
     * 获取要转换的值
     * 
     * @return 要转换的值
     */
    public IRValue getValue() {
        return value;
    }
    
    /**
     * 获取源类型
     * 
     * @return 源类型
     */
    public IRType getSourceType() {
        return sourceType;
    }
    
    /**
     * 获取目标类型
     * 
     * @return 目标类型
     */
    public IRType getTargetType() {
        return type;
    }
    
    /**
     * 检查类型兼容性
     * 
     * @param castType 转换类型
     * @param sourceType 源类型
     * @param targetType 目标类型
     */
    private void checkTypeCompatibility(CastType castType, IRType sourceType, IRType targetType) {
        switch (castType) {
            case SEXT:
            case ZEXT:
                if (!sourceType.isIntegerType() || !targetType.isIntegerType()) {
                    throw new IllegalArgumentException("Integer extension requires integer types");
                }
                if (sourceType.getSize() >= targetType.getSize()) {
                    throw new IllegalArgumentException("Integer extension requires target type to be larger than source type");
                }
                break;
                
            case TRUNC:
                if (!sourceType.isIntegerType() || !targetType.isIntegerType()) {
                    throw new IllegalArgumentException("Integer truncation requires integer types");
                }
                if (sourceType.getSize() <= targetType.getSize()) {
                    throw new IllegalArgumentException("Integer truncation requires source type to be larger than target type");
                }
                break;
                
            case FPEXT:
                if (!sourceType.isFloatType() || !targetType.isFloatType()) {
                    throw new IllegalArgumentException("Float extension requires float types");
                }
                if (sourceType.getSize() >= targetType.getSize()) {
                    throw new IllegalArgumentException("Float extension requires target type to be larger than source type");
                }
                break;
                
            case FPTRUNC:
                if (!sourceType.isFloatType() || !targetType.isFloatType()) {
                    throw new IllegalArgumentException("Float truncation requires float types");
                }
                if (sourceType.getSize() <= targetType.getSize()) {
                    throw new IllegalArgumentException("Float truncation requires source type to be larger than target type");
                }
                break;
                
            case SITOFP:
                if (!sourceType.isIntegerType() || !targetType.isFloatType()) {
                    throw new IllegalArgumentException("Integer to float conversion requires integer source type and float target type");
                }
                break;
                
            case FPTOSI:
                if (!sourceType.isFloatType() || !targetType.isIntegerType()) {
                    throw new IllegalArgumentException("Float to integer conversion requires float source type and integer target type");
                }
                break;
                
            case PTRTOINT:
                if (!sourceType.isPointerType() || !targetType.isIntegerType()) {
                    throw new IllegalArgumentException("Pointer to integer conversion requires pointer source type and integer target type");
                }
                break;
                
            case INTTOPTR:
                if (!sourceType.isIntegerType() || !targetType.isPointerType()) {
                    throw new IllegalArgumentException("Integer to pointer conversion requires integer source type and pointer target type");
                }
                break;
                
            case BITCAST:
                if (sourceType.getSize() != targetType.getSize()) {
                    throw new IllegalArgumentException("Bitcast requires source and target types to have the same size");
                }
                break;
        }
    }
    
    @Override
    protected String getInstructionName() {
        return castType.getSymbol();
    }
    
    @Override
    protected String getOperandsString() {
        return value + " to " + type;
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitCastInst(this);
    }
}