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
elvis = &comparison ?: 0
grouping = 100 * (200 + 300)

logical1 = true && false
logical2 = true || false