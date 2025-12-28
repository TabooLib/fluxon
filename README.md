# Fluxon

JVM scripting language runtime: lexer/parser/runtime, interpreter, and bytecode compiler.

Fluxon 是一个运行在 JVM 上的轻量脚本语言实现（解析器 + 解释器 + 字节码编译器），支持表达式化控制流、扩展函数与 `::` 上下文调用，可用于嵌入式脚本、REPL 与 JSR223。

## 模块结构

- `core`：语言核心（lexer/parser/runtime/interpreter/bytecode）
- `core-console`：CLI/REPL（脚本文件 + `-e/--eval`）
- `core-jsr223`：JSR223 ScriptEngine（`fluxon`）
- `core-web-backend`：最小化 HTTP 执行后端（可选）

## 构建与测试

```bash
./gradlew build
./gradlew test
# 过滤单个用例：
./gradlew test --tests 'org.tabooproject.fluxon.parser.ParserTest'
```

Windows 可使用 `gradlew.bat`。

## 运行（Console / REPL）

### 启动 REPL

```bash
./gradlew :core-console:run
```

### 执行脚本文件

```bash
./gradlew :core-console:run --args "<path/to/script.fs>"
```

### 执行表达式（`-e/--eval`）

`--args` 的值以 `-` 开头时，建议使用 `=` 形式传参：

```bash
./gradlew :core-console:run --args="-e 1 + 2"
./gradlew :core-console:run --args="-e print('hello')"
./gradlew :core-console:run --args="-e x = 1; print(&x + 2)"
```

脚本片段：

```fluxon
x = 1
print(&x + 2)
```

Windows PowerShell（推荐用绝对路径 + 将 Gradle 缓存放在工作区，避免默认 `%USERPROFILE%\.gradle` 的锁文件问题）：

```powershell
$script = (Resolve-Path "core-syntax.fs").Path
$env:GRADLE_USER_HOME = "$PWD/.gradle-local"  # optional
./gradlew --no-daemon :core-console:run --args "$script"
```

## 生成函数目录（catalog）

```bash
./gradlew :core:dumpFluxonCatalog
```

输出：`core/build/fluxon-functions.json`

## 嵌入式使用（Java）

```java
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

Environment env = FluxonRuntime.getInstance().newEnvironment();
CompilationContext ctx = new CompilationContext("x = 1; &x + 2");
ctx.setAllowReflectionAccess(false); // 需要 '.' 反射访问时再开启

Object result = Fluxon.eval(Fluxon.parse(env, ctx), env);
System.out.println(result); // 3
```

## 安全提示

- 反射成员访问 `.` 与 Java 构造 `new` 都是可开关能力；对不可信脚本建议保持禁用。
- `import` 可通过编译上下文禁用，并支持黑名单（见 `core/src/main/java/org/tabooproject/fluxon/compiler/CompilationContext.java`）。
