@api(bind = "java.lang.String")
def extConcat(suffix) = this() + &suffix

@api(bind = "java.lang.Integer")
def extTimes(factor) = this() * &factor