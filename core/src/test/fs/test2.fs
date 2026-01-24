@annotation(name="test", value=10)
def test(t, msg) = {
    print("Test called with argument: " + &t + " " + &msg)
    when &t {
        0 -> print("[INFO] " + &msg)
        1 -> print("[WARNING] " + &msg)
        2 -> print("[ERROR] " + &msg)
        else -> print("[UNKNOWN]: " + &msg)
    }
}
test(0, "Hello World")
test(1, "Something is not right")
test(2, "Something went wrong")

def test1() {
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

    elvis1 = &?undefined ?: 0
    elvis2 = &comparison ?: 0
    grouping = 100 * (200 + 300)

    u1 = !false
    u2 = !&grouping
    u3 = -&num1

    logical1 = true && false
    logical2 = true || false

    LIMIT=10
    sum = 0
    for i in 1..&LIMIT then {
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
    while &i < &LIMIT then {
        sum += &i
        i += 1
    }

    when &sum {
        in 0..&LIMIT -> print("Sum is between 0 and 10")
        in &LIMIT..100 -> print("Sum is between 10 and 100")
    }
    print("Sum: " + &sum)
    print("Random: " + random())

    try throw('error')
    try throw('error') finally print("ok")
    try throw('error') catch 'ok' finally print("ok")
    try throw('error') catch (e) 'ok' finally print("ok")
}

test1()