# Fluxon Java类型系统集成设计

## 1. 概述

Java类型系统集成是Fluxon实现的核心基础，它使Fluxon能够与Java生态系统无缝交互。本文档详细描述了Java类型系统集成的设计，包括预导入机制、类型表示、类型解析和类型映射等方面。

## 2. 预导入机制设计

### 2.1 基本原理

预导入机制是Fluxon与Java交互的基础，它在编译时决定所有可用的Java类型，而不是通过传统的import语句动态导入。

### 2.2 预导入配置

#### 2.2.1 默认预导入包

Fluxon默认预导入以下Java包：

```
java.lang.*
java.util.*
java.math.*
java.time.*
java.text.*
java.io.*
java.nio.file.*
```

#### 2.2.2 配置文件格式

项目可以通过配置文件扩展预导入列表：

```json
{
  "preImports": [
    "com.company.project.models.*",
    "com.company.project.utils.StringUtils",
    "org.apache.commons.lang3.*"
  ]
}
```

#### 2.2.3 配置加载流程

1. 加载默认预导入配置
2. 查找并加载项目级配置文件
3. 合并配置，解决冲突
4. 构建预导入类型表

### 2.3 预导入解析优先级

当多个预导入包含同名类型时，解析遵循以下优先级规则：

1. 完全限定名称优先于简单名称
2. 当前项目的类型优先于外部库
3. 显式列出的类型优先于通配符导入
4. 按配置文件中的顺序解析冲突

## 3. Java类型表示

### 3.1 类型表示模型

由于Fluxon不支持泛型，Java类型表示相对简化：

```java
class JavaType {
    String packageName;      // 包名
    String simpleName;       // 简单类名
    String fullQualifiedName; // 完全限定名
    boolean isPrimitive;     // 是否为原始类型
    boolean isArray;         // 是否为数组类型
    JavaType componentType;  // 数组元素类型（如果是数组）
    
    // 方法和字段信息
    List<JavaMethod> methods;
    List<JavaField> fields;
}

class JavaMethod {
    String name;
    JavaType returnType;
    List<JavaType> parameterTypes;
    boolean isStatic;
    int modifiers;  // public, private等
}

class JavaField {
    String name;
    JavaType type;
    boolean isStatic;
    int modifiers;
}
```

### 3.2 类型加载策略

#### 3.2.1 延迟加载

为了提高性能，Fluxon采用延迟加载策略：

1. 初始只加载类型的基本信息（名称、包等）
2. 方法和字段信息在首次访问时加载
3. 使用缓存减少重复加载

#### 3.2.2 类型缓存

```java
class TypeCache {
    // 按完全限定名缓存类型
    Map<String, JavaType> typesByFQN;
    
    // 按简单名称缓存类型（可能有多个同名类型）
    Map<String, List<JavaType>> typesBySimpleName;
    
    // 缓存方法
    void cacheType(JavaType type);
    JavaType findByFQN(String fqn);
    List<JavaType> findBySimpleName(String name);
    void clear();
}
```

## 4. 类型解析框架

### 4.1 类型解析器

```java
interface TypeResolver {
    // 解析类型引用
    JavaType resolveType(String typeName);
    
    // 解析方法引用
    JavaMethod resolveMethod(JavaType type, String methodName, List<JavaType> argTypes);
    
    // 解析字段引用
    JavaField resolveField(JavaType type, String fieldName);
}
```

### 4.2 解析算法

#### 4.2.1 类型名称解析

1. 检查是否为完全限定名（包含点）
   - 如果是，直接从类型缓存查找
   - 如果找不到，尝试通过反射加载
2. 如果是简单名称
   - 从当前作用域查找
   - 从预导入类型表查找
   - 如果有多个匹配，应用优先级规则
   - 如果仍有歧义，生成错误

#### 4.2.2 方法解析（含重载）

1. 获取目标类型的所有方法
2. 筛选出名称匹配的方法
3. 应用参数类型匹配规则
   - 精确匹配
   - 基本类型自动装箱/拆箱
   - 子类型兼容
4. 如果有多个匹配，选择最具体的一个
5. 如果无法确定，生成歧义错误

### 4.3 数组类型处理

```java
// 数组类型解析
JavaType resolveArrayType(JavaType componentType) {
    JavaType arrayType = new JavaType();
    arrayType.isArray = true;
    arrayType.componentType = componentType;
    arrayType.fullQualifiedName = componentType.fullQualifiedName + "[]";
    return arrayType;
}
```

## 5. 类型映射

### 5.1 Fluxon类型到Java类型的映射

| Fluxon类型 | Java类型 |
|-----------|---------|
| Int       | int/Integer |
| Float     | double/Double |
| Boolean   | boolean/Boolean |
| String    | java.lang.String |
| List      | java.util.List |
| Map       | java.util.Map |

### 5.2 类型转换规则

```java
// 类型转换检查
boolean isConvertible(JavaType sourceType, JavaType targetType) {
    // 相同类型
    if (sourceType.equals(targetType)) return true;
    
    // 基本类型之间的转换
    if (sourceType.isPrimitive && targetType.isPrimitive) {
        return checkPrimitiveConversion(sourceType, targetType);
    }
    
    // 基本类型与包装类型之间的转换（自动装箱/拆箱）
    if (isBoxingCompatible(sourceType, targetType)) return true;
    
    // 子类型到父类型的转换
    return isSubtypeOf(sourceType, targetType);
}
```

### 5.3 原始类型处理

由于Fluxon不支持泛型，所有泛型类型都作为原始类型处理：

```java
// 获取原始类型
JavaType getRawType(JavaType type) {
    // 如果是数组，获取组件类型的原始类型
    if (type.isArray) {
        JavaType rawComponentType = getRawType(type.componentType);
        return createArrayType(rawComponentType);
    }
    
    // 对于普通类型，直接返回（Fluxon不支持泛型）
    return type;
}
```

## 6. 实现计划

### 6.1 第一阶段：基础框架

1. 实现类型表示模型
2. 实现预导入配置加载
3. 构建基本类型缓存

### 6.2 第二阶段：类型解析

1. 实现类型名称解析
2. 实现方法解析（含重载）
3. 实现字段解析

### 6.3 第三阶段：类型映射与转换

1. 实现Fluxon类型到Java类型的映射
2. 实现类型转换规则
3. 实现原始类型处理

### 6.4 第四阶段：集成与测试

1. 与词法分析器和语法分析器集成
2. 编写单元测试和集成测试
3. 性能优化和调优

## 7. 关键挑战与解决方案

### 7.1 类型名称冲突

**挑战**：多个包中可能存在同名类型

**解决方案**：
- 实现优先级规则
- 提供明确的错误信息
- 支持使用完全限定名解决冲突

### 7.2 方法重载解析

**挑战**：在无泛型环境下正确解析重载方法

**解决方案**：
- 实现基于参数类型的匹配算法
- 考虑自动装箱/拆箱和子类型兼容性
- 在歧义情况下提供清晰的错误信息

### 7.3 性能考量

**挑战**：类型解析可能成为性能瓶颈

**解决方案**：
- 实现多级缓存策略
- 采用延迟加载机制
- 预计算常用类型信息