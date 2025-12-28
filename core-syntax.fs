import 'fs:time'

// 变量与引用
x = 10
y = &x + 5
print("y=" + &y)

// 无引号标识符文本（表达式位置默认是字符串字面量，而不是变量引用）
color = red
服务器状态 = 正常
log-level = info
print("color=" + &color)
print("status=" + &服务器状态)
print("log-level=" + &log-level)

// 字面量与集合
n = 42
longValue = 123L
floatValue = 0.5f
doubleValue = 2.5e3
s1 = "hello"
s2 = 'world'
s3 = "Line1\nLine2"
list = [1, 2, 3]
map = [host: "localhost", "port": 8080]
print("map.host=" + &map["host"])
print("list[0]=" + &list[0])

// 区间（range）与扩展函数（::）
r1 = 1..3
r2 = 0..<3
print("r1.join=" + (&r1 :: join(",")))
print("r2.join=" + (&r2 :: join(",")))

// if / when
grade = if 95 >= 90 then "A" else "B"
print("grade=" + &grade)

label = when {
    &n % 2 == 0 -> "even"
    else -> "odd"
}
print("label=" + &label)

bucket = when &y {
    in 0..10 -> "small"
    in 11..100 -> "medium"
    else -> "large"
}
print("bucket=" + &bucket)

// 循环与 break/continue
sum = 0
for i in 1..5 {
    if &i == 3 { continue }
    sum += &i
    if &sum > 10 { break }
}
print("sum=" + &sum)

j = 0
while &j < 3 {
    j += 1
}
print("j=" + &j)

// 函数定义（仅顶层）+ 注解
@api
def add(a, b) = &a + &b

@annotation(name="demo", value=1)
def fib(n) = if &n <= 1 then 1 else fib(&n - 1) + fib(&n - 2)

def abs(n) = {
    if &n >= 0 { return &n }
    return -&n
}

print("add=" + add(1, 2))
print("abs=" + abs(-3))
print("fib=" + fib(5))

// Lambda
inc = |x| &x + 1
answer = || 42
print("inc(5)=" + call(&inc, [5]))
print("answer()=" + call(&answer, []))

// 上下文调用 ::（左侧标识符可省略 ()）
items = [1, 2, 3]
itemsSize = &items :: size()
itemsStr = &items :: toString()
print("items.size=" + &itemsSize)
print("items.toString=" + &itemsStr)

timeText = time :: formatTimestamp(0L)
print("timeText=" + &timeText)

timeBlock = time :: {
    formatted = formatTimestamp(0L, "yyyy-MM-dd")
    &formatted
}
print("timeBlock=" + &timeBlock)

// 成员访问 .（反射；console 默认开启）
text = "hi"
print("text.length=" + &text.length())

// 解构赋值
pair = [10, 20]
(a, b) = &pair
print("a=" + &a)
print("b=" + &b)

kv = [k: "v"]
for (key, value) in &kv {
    print("kv=" + &key + ":" + &value)
}

// try/catch/finally
result = try {
    throw("boom")
} catch (e) {
    "caught: " + &e.message
} finally {
    print("finally")
}
print("result=" + &result)
