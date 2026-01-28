import 'fs:reflect'

def instance(name) = {
    find = forName(&name)
    return &find :: declaredField("INSTANCE") :: get(&find)
}

// 输出扩展函数
print(env::rootExtensionFunctions()::keySet()::size())
print(env::rootExtensionFunctions()::keySet())

instance("org.tabooproject.fluxon.runtime.FluxonRuntime")

_code = 0
print(static (org.tabooproject.fluxon.util.StringUtils).getCode("1234", &_code))