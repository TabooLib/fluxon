# Fluxon 与 Java 互操作性设计

## 1. Java互操作性概述

作为Java平台的领域特定语言(DSL)，Fluxon提供了与Java生态系统无缝集成的能力。这种互操作性允许Fluxon代码访问Java类型、调用Java方法、使用Java库，同时保持Fluxon自身的语法特性和简洁性。

## 2. Java类型访问

### 2.1 预导入机制
Fluxon不使用传统的import语句，而是采用"预导入"机制，在编译时决定所有可用的Java类型：

```
// 不需要import语句
val list = ArrayList<String>()  // 直接使用预导入的Java类
val today = LocalDate.now()     // 所有支持的Java类都是预导入的
```
预

```

### 2.2 Java类型引用

预导入的Java类型可以直接在代码中使用：

```
val list = ArrayList<String>()
val today = LocalDate.now()
```

在类型注解中也可以使用Java类型：

```
def process(data: ArrayList): String = {
    // 处理数据
}
```

## 3. Java方法调用

### 3.1 实例方法调用

Fluxon允许调用Java对象的方法，使用点表示法：

```
val list = ArrayList()
list.add("Hello")
list.add("World")
print(list.size())  // 输出 2
```

### 3.2 静态方法调用

同样支持调用Java类的静态方法：

```
val max = Math.max(5, 10)
val uuid = UUID.randomUUID()
```

### 3.3 方法链式调用

支持Java常见的方法链式调用模式：

```
val result = StringBuilder()
    .append("Hello")
    .append(", ")
    .append("World")
    .toString()
```

## 4. Java属性访问

### 4.1 Getter/Setter方法

Fluxon自动将Java Bean风格的getter/setter方法转换为属性访问语法：

```
// Java类
public class Person {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

// Fluxon代码
val person = Person()
person.name = "Alice"  // 调用 setName("Alice")
print(person.name)     // 调用 getName()
```

### 4.2 公共字段访问

直接支持访问Java类的公共字段：

```
// Java类
public class Point {
    public int x;
    public int y;
}

// Fluxon代码
val point = Point()
point.x = 10
point.y = 20
```

## 5. Java集合与流操作

### 5.1 集合操作

Fluxon提供了与Java集合交互的简化语法：

```
val javaList = ArrayList()
javaList.add("a")
javaList.add("b")

// 使用Fluxon语法遍历Java集合
for item in javaList {
    print(&item)
}

// 使用Fluxon列表字面量创建Java集合
val newList = ArrayList()
```

### 5.2 流操作

支持Java 8+的流API，并提供简化语法：

```
val result = people
    .stream()
    .filter(p -> p.age > 18)
    .map(p -> p.name)
    .collect(Collectors.toList())
```

## 6. 异常处理

### 6.1 简单异常处理

Fluxon提供基本的异常处理机制，但不支持高级异常捕获：

```
try {
    // 可能抛出异常的代码
    riskyOperation()
} catch {
    // 异常处理代码
    print("An error occurred")
}
```

注意事项：
- 不支持捕获特定类型的异常
- 不能访问异常对象的属性
- 所有异常都被统一处理

### 6.2 简化的错误处理

为了简化错误处理，Fluxon提供了一些辅助函数：

```
val result = tryOrNull {
    riskyOperation()
}

if &result == null {
    print("Operation failed")
}
```

## 7. 类型系统限制

### 7.1 无泛型支持

Fluxon支不持泛型，这对Java互操作性有以下影响：

1. **使用原始类型**：所有Java泛型类型都作为原始类型使用
   ```
   // 在Fluxon中
   val list = ArrayList()  // 使用原始类型，没有类型参数
   ```

2. **类型安全性降低**：没有编译时类型检查保护
   ```
   
val list = ArrayList()
   
list.add("string")
   list.add(123)  // 允许混合类型，但可能导致运行时错误
   ```

3. **类型转换**：可能需要显式类型转换
   ```
   val obj = list.get(0)
   val str = obj.toString()  // 需要使用通用方法
   ```

### 7.2 与Java泛型API交互

当与Java泛型API交互时，需要注意：

1. Java泛型方法可以被调用，但类型参数会被擦除
2. 返回的泛型类型会被视为其原始类型
3. 需要格外小心类型安全问题

## 8. 解析器实现

### 8.1 Java类型解析

语法分析器需要特殊处理预导入的Java类型引用：

1. 维护预导入类型的符号表
2. 解析限定名称（带点的标识符）
3. 解析泛型类型参数
4. 处理Java数组类型

```
// 语法规则
javaType → qualifiedName ("<" typeArguments ">")?
typeArguments → type ("," type)*
```

注意：虽然语法上支持类似泛型的语法，但这只是为了解析Java类型引用，Fluxon本身不支持泛型。

预导入类型的解析过程：

1. 当遇到标识符时，首先在当前作用域查找
2. 如果未找到，则在预导入类型表中查找
3. 对于限定名称（如`java.util.List`），按层次解析每个部分
4. 如果解析失败，生成适当的错误消息，可能包括建议的类型名

### 8.2 方法调用解析

解析Java方法调用时，需要考虑：

1. 方法重载解析
2. 可变参数方法
3. 静态方法与实例方法的区分

```java
// 方法调用AST节点
class MethodCallExpr extends Expression {
    final Expression receiver;  // 接收者对象或类
    final String methodName;    // 方法名
    final List<Expression> arguments;  // 参数
    
    // 在语义分析阶段解析具体的方法签名
    MethodSignature resolvedMethod;
    
    // ...
}
```

### 8.3 属性访问解析

解析属性访问时，需要区分：

1. 字段访问
2. Getter/Setter方法调用
3. 链式方法调用

```java
// 属性访问AST节点
class PropertyAccessExpr extends Expression {
    final Expression object;
    final String propertyName;
    
    // 在语义分析阶段确定是字段访问还是方法调用
    boolean isField;
    
    // ...
}
```

## 9. 类型系统集成

### 9.1 类型映射

Fluxon将Java类型映射到其内部类型系统：

| Java类型 | Fluxon类型 |
|---------|-----------|
| int, byte, short | Int |
| long | Long |
| float, double | Float |
| boolean | Boolean |
| char | Char |
| String | String |
| 其他引用类型 | 对应的Java类型 |

### 9.2 类型转换

在Fluxon和Java类型之间进行自动转换：

1. 基本类型的装箱和拆箱
2. 字符串与基本类型的转换
3. 集合类型的适配

```
// 自动装箱
val javaInteger: Integer = 42  // Int -> Integer

// 自动拆箱
val fluxonInt: Int = javaInteger  // Integer -> Int

// 集合适配
val fluxonList = [1, 2, 3]
val javaList: List<Integer> = &fluxonList  // 转换为Java List
```

## 10. 反射与动态调用

### 10.1 动态类型解析

Fluxon支持在运行时动态解析Java类型：

```
val className = "java.util.ArrayList"
val listClass = Class.forName(&className)
val list = &listClass.getDeclaredConstructor().newInstance()
```

### 10.2 反射API简化

提供简化的反射API，使Java反射更易于使用：

```
val methods = reflect.getMethods(Person.class)
val fields = reflect.getFields(Person.class)

// 动态调用
val result = reflect.invoke(obj, "methodName", arg1, arg2)
```

## 11. 互操作性限制

尽管Fluxon提供了强大的Java互操作性，但仍有一些限制：

1. 不支持动态导入Java类（只能使用预导入的类型）
2. 不支持直接定义Java类或接口
3. 不支持泛型
4. 不支持高级异常捕获
5. 不支持直接实现Java接口（需要通过代理机制）
6. 某些Java高级特性（如注解处理）可能受限

这些限制是为了保持Fluxon作为DSL的简洁性和专注性，同时仍然提供足够的互操作能力。

## 12. 预导入配置

### 12.1 默认预导入

Fluxon编译器默认预导入以下Java包：

```
java.lang.*
java.util.*
java.math.*
java.time.*
java.text.*
java.io.*
java.nio.file.*
```

### 12.2 自定义预导入

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

### 12.3 预导入解析优先级

当多个预导入包含同名类型时，解析遵循以下优先级规则：

1. 完全限定名称优先于简单名称
2. 当前项目的类型优先于外部库
3. 显式列出的类型优先于通配符导入
4. 按配置文件中的顺序解析冲突

编译器会检测并报告潜在的类型名称冲突。