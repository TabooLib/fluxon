@api
def libHello(name) = "Hello, " + &name

def hiddenHelper() = "hidden"

@api
def libAdd(a, b) = &a + &b