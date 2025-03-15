package org.tabooproject.fluxon.semantic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 类型信息
 * 表示变量、表达式等的类型
 */
public class TypeInfo {
    private final TypeKind kind;
    private final String name;
    private final List<TypeInfo> typeParameters; // 用于泛型类型
    
    // 预定义的基本类型
    public static final TypeInfo INT = new TypeInfo(TypeKind.PRIMITIVE, "Int");
    public static final TypeInfo FLOAT = new TypeInfo(TypeKind.PRIMITIVE, "Float");
    public static final TypeInfo BOOLEAN = new TypeInfo(TypeKind.PRIMITIVE, "Boolean");
    public static final TypeInfo STRING = new TypeInfo(TypeKind.PRIMITIVE, "String");
    public static final TypeInfo VOID = new TypeInfo(TypeKind.PRIMITIVE, "Void");
    public static final TypeInfo ANY = new TypeInfo(TypeKind.ANY, "Any");
    public static final TypeInfo UNKNOWN = new TypeInfo(TypeKind.UNKNOWN, "Unknown");
    
    /**
     * 创建类型信息
     * 
     * @param kind 类型种类
     * @param name 类型名
     */
    public TypeInfo(TypeKind kind, String name) {
        this(kind, name, new ArrayList<>());
    }
    
    /**
     * 创建类型信息
     * 
     * @param kind 类型种类
     * @param name 类型名
     * @param typeParameters 类型参数
     */
    public TypeInfo(TypeKind kind, String name, List<TypeInfo> typeParameters) {
        this.kind = kind;
        this.name = name;
        this.typeParameters = typeParameters;
    }
    
    /**
     * 获取类型种类
     * 
     * @return 类型种类
     */
    public TypeKind getKind() {
        return kind;
    }
    
    /**
     * 获取类型名
     * 
     * @return 类型名
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取类型参数
     * 
     * @return 类型参数
     */
    public List<TypeInfo> getTypeParameters() {
        return typeParameters;
    }
    
    /**
     * 创建列表类型
     * 
     * @param elementType 元素类型
     * @return 列表类型
     */
    public static TypeInfo listOf(TypeInfo elementType) {
        List<TypeInfo> typeParams = new ArrayList<>();
        typeParams.add(elementType);
        return new TypeInfo(TypeKind.LIST, "List", typeParams);
    }
    
    /**
     * 创建映射类型
     * 
     * @param keyType 键类型
     * @param valueType 值类型
     * @return 映射类型
     */
    public static TypeInfo mapOf(TypeInfo keyType, TypeInfo valueType) {
        List<TypeInfo> typeParams = new ArrayList<>();
        typeParams.add(keyType);
        typeParams.add(valueType);
        return new TypeInfo(TypeKind.MAP, "Map", typeParams);
    }
    
    /**
     * 创建函数类型
     * 
     * @param paramTypes 参数类型列表
     * @param returnType 返回类型
     * @return 函数类型
     */
    public static TypeInfo functionOf(List<TypeInfo> paramTypes, TypeInfo returnType) {
        List<TypeInfo> typeParams = new ArrayList<>(paramTypes);
        typeParams.add(returnType);
        return new TypeInfo(TypeKind.FUNCTION, "Function", typeParams);
    }
    
    /**
     * 检查类型是否可赋值给目标类型
     * 
     * @param target 目标类型
     * @return 是否可赋值
     */
    public boolean isAssignableTo(TypeInfo target) {
        // Any 类型可以接受任何类型
        if (target.kind == TypeKind.ANY) {
            return true;
        }
        
        // Unknown 类型不能赋值给任何类型（除了 Any）
        if (this.kind == TypeKind.UNKNOWN) {
            return false;
        }
        
        // 相同类型可以赋值
        if (this.equals(target)) {
            return true;
        }
        
        // TODO: 实现更复杂的类型兼容性检查，如子类型、自动装箱/拆箱等
        
        return false;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeInfo typeInfo = (TypeInfo) o;
        return kind == typeInfo.kind &&
                Objects.equals(name, typeInfo.name) &&
                Objects.equals(typeParameters, typeInfo.typeParameters);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(kind, name, typeParameters);
    }
    
    @Override
    public String toString() {
        if (typeParameters.isEmpty()) {
            return name;
        }
        
        StringBuilder sb = new StringBuilder(name);
        sb.append("<");
        
        for (int i = 0; i < typeParameters.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(typeParameters.get(i));
        }
        
        sb.append(">");
        return sb.toString();
    }
    
    /**
     * 类型种类
     */
    public enum TypeKind {
        /**
         * 原始类型（Int, Float, Boolean, String 等）
         */
        PRIMITIVE,
        
        /**
         * 列表类型
         */
        LIST,
        
        /**
         * 映射类型
         */
        MAP,
        
        /**
         * 函数类型
         */
        FUNCTION,
        
        /**
         * Any 类型（可以接受任何类型）
         */
        ANY,
        
        /**
         * 未知类型（用于类型推断失败时）
         */
        UNKNOWN
    }
}