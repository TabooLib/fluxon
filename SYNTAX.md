# Fluxon 语言语法文档

## 目录
1. [简介](#简介)
2. [注释](#注释)
3. [字面量](#字面量)
4. [变量](#变量)
5. [运算符](#运算符)
6. [函数](#函数)
7. [控制流](#控制流)
8. [表达式](#表达式)
9. [内置函数](#内置函数)
10. [特殊特性](#特殊特性)

## 简介

Fluxon 是一门运行在 JVM 上的动态、以表达式为中心的脚本语言。它支持动态类型、一等函数、模式匹配以及异步编程等特性。

## 注释

Fluxon 使用 `//` 表示单行注释：

```fluxon
// 这是一条注释
x = 5 // 这是一条行尾注释
```

## 字面量

### 数字

```fluxon
// 整数字面量
42
-10
0

// 长整型字面量（后缀 L 或 l）
0L
123l
42L
9876543210l

// 浮点数字面量（后缀 f 或 F）
0.5f
3.14F
1.0f

// 双精度字面量（无后缀或使用 d/D）
0.5
3.14
1.0
1e10
2.5e3
1.2e-5
3e+2
```

### 字符串

字符串可以使用双引号或单引号：

```fluxon
"hello"
"world"
'hello'
'world'

// 转义序列
"\\n\\r\\t\\\\\\"\\\'"
'\\n\\r\\t\\\\\\"\\\''

// 空字符串
""
''
```

### 布尔值

```fluxon
true
false
```

### 空值

```fluxon
null
```

### 列表

列表使用方括号：

```fluxon
[1, 2, 3]
["a", "b", "c"]
[]  // 空列表
[[1, 2], [3, 4], [5, 6]]  // 嵌套列表
```

### 映射

映射使用方括号和键值对：

```fluxon
[a: 1, b: 2, c: 3]
["key": "value", "another": 42]
```

## 变量

### 变量声明

```fluxon
x = 5
msg = "hello"
counter = 0
result = 'fail'
```

### 变量引用

使用 `&` 引用变量：

```fluxon
x = 10
y = &x + 5  // 引用变量 x
result = &result + &i  // 同时引用多个变量
```

### 赋值

```fluxon
// 基本赋值
x = 10
result = 'ok'

// 复合赋值
result += &i
counter += 1
output *= 2
```

## 运算符

### 算术运算符

```fluxon
+ - * / %

// 示例
&n * factorial(&n - 1)
&x + &y
&i % 2
```

### 比较运算符

```fluxon
== != > < >= <=

// 示例
&n <= 1
&i == 5
&count >= 5
&sum > 50
```

### 逻辑运算符

```fluxon
&& || !

// 示例
&x > 0 && &x < 10
condition1 || condition2
!isValid
```

### 赋值运算符

```fluxon
= += -= *= /=

// 示例
result = 0
result += &i
count -= 1
```

### 特殊运算符

```fluxon
// 上下文调用（在对象上调用方法）
hash::md5("data")
base64::encode("text")
time::now()

// 区间运算符
1..10    // 闭区间
1..<10   // 右开区间

// Elvis 运算符
x ?: defaultValue

// 引用运算符
&variable
```

## 函数

### 函数定义

```fluxon
// 简单函数定义
def factorial(n) = &n

// 使用表达式体的函数
def factorial(n) = if &n <= 1 then 1 else &n * factorial(&n - 1)

// 使用代码块作为函数体
def max(x, y) = { if (&x > &y) &x else &y }

// 异步函数定义
async def test = { return 42 }
async def loadData = { sleep 10 return 'ok' }
```

### 函数调用

```fluxon
// 使用括号的普通函数调用
print("hello")
max(10, 20)
factorial(5)

// 使用 :: 的上下文调用
hash::md5("Hello")
base64::encode("Hello, World!")
hex::decode("48656c6c6f")
time::formatDateTime(now)
```

## 控制流

### If-Then-Else 表达式

```fluxon
// 简单 if 表达式
if &n <= 1 then 1 else &n * factorial(&n - 1)

// 带复杂条件的 if
if &x % 2 == 0 then "even" else "odd"

// 嵌套 if
if condition1 then
    if condition2 then result1 else result2
else
    result3
```

### When 表达式

```fluxon
// 无主体的 when（基于条件）
when {
    &num % 2 == 0 -> "even"
    &num < 0 -> "negative odd"
    else -> "positive odd"
}

// 带主体的 when
when x {
    1 -> "one"
    2 -> "two"
    else -> "other"
}

// 带复杂条件的 when
when {
    true -> 1
    false -> 0
}
```

### For 循环

```fluxon
// 遍历列表的 for 循环
for i in [1, 2, 3] {
    result += &i
}

// 遍历区间的 for 循环
for i in 1..10 {
    print &i
}

// 带解构的 for 循环
for (key, value) in &map {
    result = &result + &key + &value
}

// 嵌套 for 循环
for i in 1..3 {
    for j in 1..3 {
        result += (&i * &j)
    }
}
```

### While 循环

```fluxon
// 基本 while 循环
while &count < 10 {
    count = &count + 1
    output = &output + &count + ','
}

// 使用 break 的无限循环
while true {
    output = &output + &count + ','
    count = &count + 1
    if &count >= 5 {
        break
    }
}
```

### Break 与 Continue

```fluxon
// break 语句
for i in 1..10 {
    if &i == 5 {
        result = 'ok'
        break
    }
    print &i
}

// continue 语句
for i in 1..10 {
    if &i % 2 == 0 {
        continue
    }
    output = &output + &i
}
```

### Return 语句

```fluxon
// 带返回值的 return
return 42
return 'ok'
return &result

// 在异步函数中
async def test = { sleep 10 return 'ok' }
```

## 表达式

在 Fluxon 中，一切都是表达式：

```fluxon
// 变量声明会返回该值
x = 10

// if 表达式会返回值
result = if &x > 0 then "positive" else "negative"

// for 循环可以用在表达式上下文中
for i in 1..4 { result += &i }; &result

// 序列中最后一个表达式的值会被返回
result = 0; for i in [1, 2, 3] { result += &i }; &result
```

## 内置函数

### 系统函数

```fluxon
print value        // 输出到标准输出
print("hello")     // 使用括号调用
print &variable    // 打印变量

error message      // 输出到标准错误
sleep milliseconds // 休眠指定毫秒
forName className  // 加载 Java 类
now                // 获取当前时间戳（0 个参数）
```

### 数学函数

```fluxon
min(a, b)
max(a, b)
abs(x)
round(x)
floor(x)
ceil(x)
pow(x, y)
sqrt(x)
sin(x)
cos(x)
tan(x)
exp(x)
log(x)
random()
```

### 时间函数

```fluxon
time                           // 时间对象
time::now()                    // 当前时间戳
time::formatDateTime(millis)   // 格式化时间戳
```

### 编码函数

```fluxon
// 哈希函数（返回哈希对象，再调用其方法）
hash::md5("Hello")
hash::sha1("Hello")
hash::sha256("Hello")
hash::sha384("Hello")
hash::sha512("Hello")

// Base64 编码/解码
base64::encode("Hello, World!")
base64::decode("SGVsbG8sIFdvcmxkIQ==")

// 十六进制编码/解码
hex::encode("Hello")
hex::decode("48656c6c6f")

// Unicode 编码/解码
unicode::encode("Hello 你好")
unicode::decode("Hello \\u4f60\\u597d")
```

## 特殊特性

### 字符串拼接

```fluxon
// 使用 + 运算符
output = &output + &i + ','
result = &result + &key + &value
"Hello " + name
```

### 分号作为语句分隔符

```fluxon
// 一行中写多个语句
result = 0; for i in [1, 2, 3] { result += &i }; &result

// 行尾分号是可选的
x = 10
y = 20
```

### Async/Await

```fluxon
// 定义异步函数
async def fetchData = {
    sleep 100
    return "data"
}

// 等待异步函数
await fetchData

// 对非异步值使用 await（会立即返回）
await 123
```

### 表达式序列

```fluxon
// 多个表达式由换行或分号分隔
start = now
end = now + 1000
diff = &end - &start
return &diff
```

## 示例

### 阶乘函数

```fluxon
def factorial(n) = if &n <= 1 then 1 else &n * factorial(&n - 1)
```

### 字符串处理

```fluxon
text = "Hello, World!"
encoded = base64::encode(&text)
decoded = base64::decode(&encoded)
```

### 带 break 的循环

```fluxon
result = 'fail'
for i in 1..10 {
    if &i == 5 {
        result = 'ok'
        break
    }
    print &i
}
```

### 过滤奇数

```fluxon
output = ''
for i in 1..10 {
    if &i % 2 == 0 {
        continue
    }
    output = &output + &i
}
```

### 复杂表达式

```fluxon
def max(x, y) = { if (&x > &y) &x else &y }
a = 10
b = 5
result = max(&a + &b, &a * &b)  // max(15, 50) = 50
```