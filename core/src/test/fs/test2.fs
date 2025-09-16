@annotation(name="test", value=10)
def test t msg = {
    print "Test called with argument: " + &t + " " + &msg
    when &t {
        0 -> print "[INFO] " + &msg
        1 -> print "[WARNING] " + &msg
        2 -> print "[ERROR] " + &msg
        else -> print "[UNKNOWN]: " + &msg
    }
}
test 0 "Hello World"
test 1 "Something is not right"
test 2 "Something went wrong"