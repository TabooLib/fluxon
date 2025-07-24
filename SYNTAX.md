# Fluxon Language Syntax Documentation

## Table of Contents
1. [Introduction](#introduction)
2. [Comments](#comments)
3. [Literals](#literals)
4. [Variables](#variables)
5. [Operators](#operators)
6. [Functions](#functions)
7. [Control Flow](#control-flow)
8. [Expressions](#expressions)
9. [Built-in Functions](#built-in-functions)
10. [Special Features](#special-features)

## Introduction

Fluxon is a dynamic, expression-oriented scripting language for the JVM. It features dynamic typing, first-class functions, pattern matching, and asynchronous programming support.

## Comments

Fluxon supports single-line comments with `//`:

```fluxon
// This is a comment
x = 5 // This is an inline comment
```

## Literals

### Numbers

```fluxon
// Integer literals
42
-10
0

// Long literals (suffix L or l)
0L
123l
42L
9876543210l

// Float literals (suffix f or F)
0.5f
3.14F
1.0f

// Double literals (no suffix or with d/D)
0.5
3.14
1.0
1e10
2.5e3
1.2e-5
3e+2
```

### Strings

Strings can use double quotes or single quotes:

```fluxon
"hello"
"world"
'hello'
'world'

// Escape sequences
"\\n\\r\\t\\\\\\\"\\\'"
'\\n\\r\\t\\\\\\\"\\\''

// Empty strings
""
''
```

### Booleans

```fluxon
true
false
```

### Null

```fluxon
null
```

### Lists

Lists use square brackets:

```fluxon
[1, 2, 3]
["a", "b", "c"]
[]  // Empty list
[[1, 2], [3, 4], [5, 6]]  // Nested lists
```

### Maps

Maps use square brackets with key-value pairs:

```fluxon
[a: 1, b: 2, c: 3]
["key": "value", "another": 42]
```

## Variables

### Variable Declaration

```fluxon
x = 5
msg = "hello"
counter = 0
result = 'fail'
```

### Variable References

Use `&` to reference a variable:

```fluxon
x = 10
y = &x + 5  // Reference x
result = &result + &i  // Reference both variables
```

### Assignment

```fluxon
// Basic assignment
x = 10
result = 'ok'

// Compound assignment
result += &i
counter += 1
output *= 2
```

## Operators

### Arithmetic Operators

```fluxon
+ - * / %

// Examples
&n * factorial(&n - 1)
&x + &y
&i % 2
```

### Comparison Operators

```fluxon
== != > < >= <=

// Examples
&n <= 1
&i == 5
&count >= 5
&sum > 50
```

### Logical Operators

```fluxon
&& || !

// Examples
&x > 0 && &x < 10
condition1 || condition2
!isValid
```

### Assignment Operators

```fluxon
= += -= *= /=

// Examples
result = 0
result += &i
count -= 1
```

### Special Operators

```fluxon
// Member access
obj.field

// Context call (method on object)
hash::md5("data")
base64::encode("text")
time::now()

// Range operators
1..10    // Inclusive range
1..<10   // Exclusive end

// Elvis operator
x ?: defaultValue

// Reference operator
&variable
```

## Functions

### Function Definition

```fluxon
// Simple function definition
def factorial(n) = &n

// Function with expression body
def factorial(n) = if &n <= 1 then 1 else &n * factorial(&n - 1)

// Function with block body
def max(x, y) = { if (&x > &y) &x else &y }

// Async function definition
async def test = { return 42 }
async def loadData = { sleep 10 return 'ok' }
```

### Function Calls

```fluxon
// Regular function call with parentheses
print("hello")
max(10, 20)
factorial(5)

// No-bracket function call
print "hello"
print 123
max 10 20
msg 嗯嗯啊啊

// Nested no-bracket calls
print checkGrade 85
double inc 5

// Method calls with context operator ::
hash::md5("Hello")
base64::encode("Hello, World!")
hex::decode("48656c6c6f")
time::formatDateTime(now)
```

### Lambda Expressions

```fluxon
// Lambda with arrow operator
(x) -> x * 2
(a, b) -> a + b
```

## Control Flow

### If-Then-Else Expressions

```fluxon
// Simple if expression
if &n <= 1 then 1 else &n * factorial(&n - 1)

// If with complex conditions
if &x % 2 == 0 then "even" else "odd"

// Nested if
if condition1 then
    if condition2 then result1 else result2
else
    result3
```

### When Expressions

```fluxon
// When without subject (condition-based)
when {
    &num % 2 == 0 -> "even"
    &num < 0 -> "negative odd"
    else -> "positive odd"
}

// When with subject
when x {
    1 -> "one"
    2 -> "two"
    else -> "other"
}

// When with complex conditions
when {
    true -> 1
    false -> 0
}
```

### For Loops

```fluxon
// For loop over list
for i in [1, 2, 3] {
    result += &i
}

// For loop over range
for i in 1..10 {
    print &i
}

// For loop with destructuring
for (key, value) in &map {
    result = &result + &key + &value
}

// Nested for loops
for i in 1..3 {
    for j in 1..3 {
        result += (&i * &j)
    }
}
```

### While Loops

```fluxon
// Basic while loop
while &count < 10 {
    count = &count + 1
    output = &output + &count + ','
}

// Infinite loop with break
while true {
    output = &output + &count + ','
    count = &count + 1
    if &count >= 5 {
        break
    }
}
```

### Break and Continue

```fluxon
// Break statement
for i in 1..10 {
    if &i == 5 {
        result = 'ok'
        break
    }
    print &i
}

// Continue statement
for i in 1..10 {
    if &i % 2 == 0 {
        continue
    }
    output = &output + &i
}
```

### Return Statement

```fluxon
// Return with value
return 42
return 'ok'
return &result

// In async function
async def test = { sleep 10 return 'ok' }
```

## Expressions

Everything in Fluxon is an expression:

```fluxon
// Variable declaration returns the value
x = 10

// If expression returns a value
result = if &x > 0 then "positive" else "negative"

// For loop can be used in expression context
for i in 1..4 { result += &i }; &result

// The last expression in a sequence is returned
result = 0; for i in [1, 2, 3] { result += &i }; &result
```

## Built-in Functions

### System Functions

```fluxon
print value        // Print to stdout
print("hello")     // With parentheses
print &variable    // Print variable

error message      // Print to stderr
sleep milliseconds // Sleep for specified time
forName className  // Load Java class
now                // Get current timestamp (0 parameters)
```

### Math Functions

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

### Time Functions

```fluxon
time                           // Time object
time::now()                    // Current timestamp
time::formatDateTime(millis)   // Format timestamp
```

### Encoding Functions

```fluxon
// Hash functions (return hash object, then call methods)
hash::md5("Hello")
hash::sha1("Hello")
hash::sha256("Hello")
hash::sha384("Hello")
hash::sha512("Hello")

// Base64 encoding/decoding
base64::encode("Hello, World!")
base64::decode("SGVsbG8sIFdvcmxkIQ==")

// Hex encoding/decoding
hex::encode("Hello")
hex::decode("48656c6c6f")

// Unicode encoding/decoding
unicode::encode("Hello 你好")
unicode::decode("Hello \\u4f60\\u597d")
```

## Special Features

### String Concatenation

```fluxon
// Using + operator
output = &output + &i + ','
result = &result + &key + &value
"Hello " + name
```

### Semicolon as Statement Separator

```fluxon
// Multiple statements on one line
result = 0; for i in [1, 2, 3] { result += &i }; &result

// Semicolons are optional at end of line
x = 10
y = 20
```

### Async/Await

```fluxon
// Define async function
async def fetchData = {
    sleep 100
    return "data"
}

// Await async function
await fetchData

// Await non-async value (returns immediately)
await 123
```

### Expression Sequences

```fluxon
// Multiple expressions separated by newlines or semicolons
start = now
end = now + 1000
diff = &end - &start
return &diff
```

### No-Bracket Function Calls

Functions can be called without parentheses when unambiguous:

```fluxon
// Single argument
print "hello"
print 123

// Multiple arguments  
max 10 20

// Nested calls
print max 10 20
double inc 5

// With expressions
print &a - &b
result = double &a + 3
```

## Examples

### Factorial Function

```fluxon
def factorial(n) = if &n <= 1 then 1 else &n * factorial(&n - 1)
```

### String Processing

```fluxon
text = "Hello, World!"
encoded = base64::encode(&text)
decoded = base64::decode(&encoded)
```

### Loop with Break

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

### Filtering Odd Numbers

```fluxon
output = ''
for i in 1..10 {
    if &i % 2 == 0 {
        continue
    }
    output = &output + &i
}
```

### Complex Expression

```fluxon
def max(x, y) = { if (&x > &y) &x else &y }
a = 10
b = 5
result = max &a + &b &a * &b  // max(15, 50) = 50
```