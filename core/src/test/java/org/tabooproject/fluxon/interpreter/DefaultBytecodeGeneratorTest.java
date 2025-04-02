package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.DefaultBytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.literal.DoubleLiteral;
import org.tabooproject.fluxon.parser.expression.literal.IntLiteral;
import org.tabooproject.fluxon.runtime.Environment;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultBytecodeGeneratorTest {

    @Test
    public void testSimpleMathOperation() throws Exception {
        // 创建字节码生成器
        BytecodeGenerator generator = new DefaultBytecodeGenerator();

        // 添加字符串 Field
//        generator.addField("text", "Ljava/lang/String;", new BinaryExpression(
//                new StringLiteral("Hello"),
//                new Token(TokenType.PLUS, "+", 1, 1),
//                new IntLiteral(999)
//        ));

        // 创建表达式: 1 + 2
//        BinaryExpression expr = new BinaryExpression(
//                new IntLiteral(1),
//                new Token(TokenType.PLUS, "+", 1, 1),
//                new IntLiteral(2)
//        );

        generator.setReturnExpression(
                new BinaryExpression(
                        new IntLiteral(1),
                        new Token(TokenType.PLUS),
                        new IntLiteral(2)
                )
        );

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