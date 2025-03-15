package org.tabooproject.fluxon.ir.type;

import org.tabooproject.fluxon.ir.IRType;

import java.util.HashMap;
import java.util.Map;

/**
 * IR 类型工厂
 * 用于创建和缓存 IR 类型
 */
public class IRTypeFactory {
    private static final IRTypeFactory INSTANCE = new IRTypeFactory();
    
    // 缓存类型实例
    private final Map<String, IRType> typeCache = new HashMap<>();
    
    // 预定义的基本类型
    private final IRType voidType = new VoidType();
    private final IRType boolType = new IntegerType("bool", 1);
    private final IRType int8Type = new IntegerType("i8", 1);
    private final IRType int16Type = new IntegerType("i16", 2);
    private final IRType int32Type = new IntegerType("i32", 4);
    private final IRType int64Type = new IntegerType("i64", 8);
    private final IRType floatType = new FloatType("float", 4);
    private final IRType doubleType = new FloatType("double", 8);
    
    /**
     * 私有构造函数
     */
    private IRTypeFactory() {
        // 初始化类型缓存
        typeCache.put("void", voidType);
        typeCache.put("bool", boolType);
        typeCache.put("i8", int8Type);
        typeCache.put("i16", int16Type);
        typeCache.put("i32", int32Type);
        typeCache.put("i64", int64Type);
        typeCache.put("float", floatType);
        typeCache.put("double", doubleType);
    }
    
    /**
     * 获取实例
     * 
     * @return 类型工厂实例
     */
    public static IRTypeFactory getInstance() {
        return INSTANCE;
    }
    
    /**
     * 获取 void 类型
     * 
     * @return void 类型
     */
    public IRType getVoidType() {
        return voidType;
    }
    
    /**
     * 获取布尔类型
     * 
     * @return 布尔类型
     */
    public IRType getBoolType() {
        return boolType;
    }
    
    /**
     * 获取 8 位整数类型
     * 
     * @return 8 位整数类型
     */
    public IRType getInt8Type() {
        return int8Type;
    }
    
    /**
     * 获取 16 位整数类型
     * 
     * @return 16 位整数类型
     */
    public IRType getInt16Type() {
        return int16Type;
    }
    
    /**
     * 获取 32 位整数类型
     * 
     * @return 32 位整数类型
     */
    public IRType getInt32Type() {
        return int32Type;
    }
    
    /**
     * 获取 64 位整数类型
     * 
     * @return 64 位整数类型
     */
    public IRType getInt64Type() {
        return int64Type;
    }
    
    /**
     * 获取单精度浮点类型
     * 
     * @return 单精度浮点类型
     */
    public IRType getFloatType() {
        return floatType;
    }
    
    /**
     * 获取双精度浮点类型
     * 
     * @return 双精度浮点类型
     */
    public IRType getDoubleType() {
        return doubleType;
    }
    
    /**
     * 获取指针类型
     * 
     * @param elementType 元素类型
     * @return 指针类型
     */
    public IRType getPointerType(IRType elementType) {
        String name = elementType.getName() + "*";
        
        // 检查缓存
        if (typeCache.containsKey(name)) {
            return typeCache.get(name);
        }
        
        // 创建新类型
        IRType type = new PointerType(elementType);
        typeCache.put(name, type);
        
        return type;
    }
    
    /**
     * 获取数组类型
     * 
     * @param elementType 元素类型
     * @param length 数组长度
     * @return 数组类型
     */
    public IRType getArrayType(IRType elementType, int length) {
        String name = elementType.getName() + "[" + length + "]";
        
        // 检查缓存
        if (typeCache.containsKey(name)) {
            return typeCache.get(name);
        }
        
        // 创建新类型
        IRType type = new ArrayType(elementType, length);
        typeCache.put(name, type);
        
        return type;
    }
    
    /**
     * 获取函数类型
     * 
     * @param returnType 返回类型
     * @param paramTypes 参数类型
     * @return 函数类型
     */
    public IRType getFunctionType(IRType returnType, IRType... paramTypes) {
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(returnType.getName()).append(" (");
        
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) {
                nameBuilder.append(", ");
            }
            nameBuilder.append(paramTypes[i].getName());
        }
        
        nameBuilder.append(")");
        String name = nameBuilder.toString();
        
        // 检查缓存
        if (typeCache.containsKey(name)) {
            return typeCache.get(name);
        }
        
        // 创建新类型
        IRType type = new FunctionType(returnType, paramTypes);
        typeCache.put(name, type);
        
        return type;
    }
    
    /**
     * 获取结构体类型
     * 
     * @param name 结构体名称
     * @param fieldTypes 字段类型
     * @return 结构体类型
     */
    public IRType getStructType(String name, IRType... fieldTypes) {
        // 检查缓存
        if (typeCache.containsKey(name)) {
            return typeCache.get(name);
        }
        
        // 创建新类型
        IRType type = new StructType(name, fieldTypes);
        typeCache.put(name, type);
        
        return type;
    }
}