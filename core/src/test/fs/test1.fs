num1 = 10
num2 = 20
max = if &num1 > &num2 then &num1 else &num2

comparison = if &num1 > &num2 then {
    greater
} else if &num1 < &num2 then {
    less
} else {
    equal
}

list = [&num1, &num2, 100, 200]
map = ["key1": 100, "key2": 200]

elvis = &comparison ?: 0
grouping = 100 * (200 + 300)

u1 = !false
u2 = !&elvis
u3 = -&num1

logical1 = true && false
logical2 = true || false

sum = 0
for i in 1..10 then {
    print &i
    if &i % 2 == 0 then {
        print("continue")
        continue
    }
    if &i == 5 then {
        print("break")
        break
    }
    sum += &i
}

i = 0
while &i < 10 then {
    sum += &i
    i += 1
}

when &sum {
    in 0..10 -> print("Sum is between 0 and 10")
    in 10..100 -> print("Sum is between 10 and 100")
}
print("Sum: " + &sum)
print("Random: " + random())

try throw('error')
try throw('error') finally print("ok")
try throw('error') catch 'ok' finally print("ok")
try throw('error') catch (e) 'ok' finally print("ok")
