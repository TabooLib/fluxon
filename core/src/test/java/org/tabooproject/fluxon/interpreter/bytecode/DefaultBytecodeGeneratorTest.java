package org.tabooproject.fluxon.interpreter.bytecode;

import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;

import java.io.File;
import java.nio.file.Files;

public class DefaultBytecodeGeneratorTest {

    public static void main(String[] args) throws Exception {
        // 创建字节码生成器
        BytecodeGenerator generator = new DefaultBytecodeGenerator();

        // 添加字符串 Field
        for (ParseResult result : Fluxon.parse("" +
                "num1 = 10;" +
                "num2 = 20;" +
                "cond1 = &num1 > &num2;" +
                "cond2 = if &num1 == 10 then &num1 else &num2;" +
                "cond3 = if &num1 == 10 then 1 else if &num1 == 20 then 2 else 3;")
        ) {
            if (result instanceof ExpressionStatement) {
                generator.addScriptBody((Statement) result);
            }
        }

        // 生成字节码
        byte[] bytecode = generator.generateClassBytecode("CompiledScript");

        // 输出脚本便于调试
        File file = new File("build/CompiledScript.class");
        file.getParentFile().mkdirs();
        file.createNewFile();
        Files.write(file.toPath(), bytecode);

        // 加载并执行
        FluxonClassLoader loader = new FluxonClassLoader();
        Class<?> scriptClass = loader.defineClass("CompiledScript", bytecode);

        Environment env = new Environment();
        scriptClass.getDeclaredConstructor(Environment.class).newInstance(env);
    }
}