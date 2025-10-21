import 'fs:reflect'

def instance(name) = {
    find = forName(&name)
    return &find :: declaredField("INSTANCE") :: get(&find)
}

// 输出扩展函数
print(env::rootExtensionFunctions()::keySet()::size())
print(env::rootExtensionFunctions()::keySet())

return instance("org.tabooproject.fluxon.runtime.FluxonRuntime")

if (true) {
    1
} else {
    a = 1
}