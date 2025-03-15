package org.tabooproject.fluxon;

import org.tabooproject.fluxon.compiler.FluxonCompiler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Fluxon 语言的主入口类
 * 提供编译和执行 Fluxon 代码的 API
 */
public class Fluxon {
    
    /**
     * 编译 Fluxon 源代码
     * 
     * @param source Fluxon 源代码
     * @return 编译后的字节码
     */
    public static byte[] compile(String source) {
        FluxonCompiler compiler = new FluxonCompiler();
        return compiler.compile(source);
    }
    
    /**
     * 编译 Fluxon 源文件
     * 
     * @param file Fluxon 源文件
     * @return 编译后的字节码
     * @throws IOException 如果文件读取失败
     */
    public static byte[] compileFile(File file) throws IOException {
        String source = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return compile(source);
    }
    
    /**
     * 执行 Fluxon 源代码
     * 
     * @param source Fluxon 源代码
     * @return 执行结果
     */
    public static Object eval(String source) {
        // 编译并加载生成的类
        byte[] bytecode = compile(source);
        // 使用自定义类加载器加载并执行
        FluxonClassLoader classLoader = new FluxonClassLoader();
        return classLoader.loadAndExecute(bytecode);
    }
    
    /**
     * Fluxon 专用类加载器
     */
    private static class FluxonClassLoader extends ClassLoader {
        public Object loadAndExecute(byte[] bytecode) {
            // 加载类并执行 main 方法或顶层代码
            // 这里简化实现，实际需要更复杂的逻辑
            return null; // 暂时返回 null
        }
    }
}