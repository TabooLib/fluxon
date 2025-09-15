list = [1, 2, 3]

print &?list :: {
    add 9
    toString
}

for i in &list then {
    print &i
}

def getScore(score) {
    &score >= 90 ? "A" : &score >= 80 ? "B" : "C"
}

score = 95;
print getScore(&score)
score = 85
print getScore(&score)
score = 75
print getScore(&score)