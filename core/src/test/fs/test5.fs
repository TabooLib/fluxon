find = forName("org.tabooproject.fluxon.runtime.FluxonRuntime")
print &find
print &find :: getDeclaredField("INSTANCE") :: get &find

print(env::rootExtensionFunctions()::keySet()::size())
print(env::rootExtensionFunctions()::keySet())