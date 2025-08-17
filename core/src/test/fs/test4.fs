list = [1, 2, 3]

print &?list :: {
    add 9
    toString
}

for i in &list then {
    print &i
}