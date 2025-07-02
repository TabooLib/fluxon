num1 = 10
num2 = 20
max = if &num1 > &num2 then {
    &num1
} else {
    &num2
}
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

logical1 = true && false
logical2 = true || false

sum = 0
for i in [1, 2, 3, 4] then {
    sum = &sum + &i
}
&sum