package org.tabooproject.fluxon;

import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;
import org.tabooproject.fluxon.compiler.FluxonFeatures;
import org.tabooproject.fluxon.jsr223.FluxonScriptEngine;
import org.tabooproject.fluxon.jsr223.FluxonScriptEngineFactory;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.util.PseudoCodeFormatter;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 简单的交互式脚本控制台
 */
public class FluxonConsole {
    private static final String PROMPT = "\u001B[32mfluxon\u001B[0m> ";
    private static final String HISTORY_FILE = ".fluxon_history";

    private final Terminal terminal;
    private final LineReader reader;
    private final List<String> scriptContent = new ArrayList<>();
    private boolean running = true;

    // 添加脚本引擎实例变量，保持上下文一致
    private final ScriptEngine scriptEngine;

    /**
     * 构造函数，初始化终端和读取器
     */
    public FluxonConsole() throws IOException {
        FluxonFeatures.DEFAULT_ALLOW_REFLECTION_ACCESS = true;
        FluxonFeatures.DEFAULT_ALLOW_JAVA_CONSTRUCTION = true;
        // 构建终端
        terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        // 历史文件路径
        Path historyPath = Paths.get(System.getProperty("user.home"), HISTORY_FILE);

        // 构建行读取器
        reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser())
                .variable(LineReader.HISTORY_FILE, historyPath)
                .build();

        // 初始化脚本引擎
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("fluxon");

        // 如果没有通过 SPI 找到引擎，则手动创建
        if (engine == null) {
            System.out.println("Failed to get Fluxon script engine, please check service provider configuration.");
            System.out.println("Trying to create engine directly...");
            engine = new FluxonScriptEngine(new FluxonScriptEngineFactory());
        }
        this.scriptEngine = engine;
    }

    /**
     * 运行脚本编辑器
     */
    public void run() {
        printWelcome();
        while (running) {
            try {
                String line = reader.readLine(PROMPT);
                processLine(line);
            } catch (UserInterruptException e) {
                clearScript();
            } catch (EndOfFileException e) {
                break;
            } catch (Exception e) {
                printError("Error: ", e);
            }
        }
        printInfo("Goodbye!");
    }

    /**
     * 处理输入行
     */
    private void processLine(String line) {
        line = line.trim();

        // 处理特殊命令
        switch (line) {
            case ":q":
            case ":quit":
            case ":exit":
                running = false;
                return;

            case ":c":
            case ":clear":
                clearScript();
                return;

            case ":l":
            case ":list":
                showScript();
                return;

            case ":h":
            case ":help":
                showHelp();
                return;

            case ":v":
            case ":vars":
                showVariables();
                return;

            case "clear":
                terminal.puts(InfoCmp.Capability.clear_screen);
                return;
        }

        // 添加到脚本内容
        if (!line.isEmpty()) {
            // 如果脚本以 "$" 开头表示输出伪代码结构
            if (line.startsWith("$")) {
                for (ParseResult parseResult : Fluxon.parse(line.substring(1))) {
                    printInfo(parseResult.toString());
                    printInfo(PseudoCodeFormatter.format(parseResult.toPseudoCode()));
                }
            } else {
                scriptContent.add(line);
                executeCommand(line);
            }
        }
    }

    /**
     * 清空脚本内容
     */
    private void clearScript() {
        scriptContent.clear();
        printInfo("Script content cleared");
    }

    /**
     * 显示当前脚本内容
     */
    private void showScript() {
        if (scriptContent.isEmpty()) {
            printInfo("Script content is empty");
            return;
        }
        printInfo("Current script content:");
        for (int i = 0; i < scriptContent.size(); i++) {
            printInfo(String.format("%3d | %s", i + 1, scriptContent.get(i)));
        }
    }

    /**
     * 显示脚本中的变量
     */
    private void showVariables() {
        try {
            printInfo("Current variables:");
            // 获取引擎的绑定对象
            Bindings bindings1 = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
            // 显示变量
            printInfo("[ENGINE_SCOPE]:");
            for (Map.Entry<String, Object> entry : bindings1.entrySet()) {
                printInfo(String.format("  %-15s = %s", entry.getKey(), entry.getValue()));
            }
            Bindings bindings2 = scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE);
            printInfo("[GLOBAL_SCOPE]:");
            for (Map.Entry<String, Object> entry : bindings2.entrySet()) {
                printInfo(String.format("  %-15s = %s", entry.getKey(), entry.getValue()));
            }
        } catch (Exception e) {
            printError("Error getting variables: ", e);
        }
    }

    /**
     * 执行命令
     */
    private void executeCommand(String command) {
        try {
            // 使用 FluxonScriptEngine 执行命令
            long startTime = System.nanoTime();
            Object result = scriptEngine.eval(command);
            long endTime = System.nanoTime();
            // 保存最后的执行结果到脚本引擎环境中
            if (result != null) {
                scriptEngine.put("_", result);
                printInfo("> " + result);
            }
            printInfo("Time taken: " + (endTime - startTime) / 1000000.0 + " ms");
        } catch (Exception e) {
            printError("Execution error: ", e);
        }
    }

    /**
     * 显示帮助信息
     */
    private void showHelp() {
        printInfo("Fluxon Console script editor commands:");
        printInfo("  :quit, :q    - Exit the script editor");
        printInfo("  :clear, :c   - Clear the current script content");
        printInfo("  :list, :l    - Display the current script content");
        printInfo("  :vars, :v    - Display current variables");
        printInfo("  :help, :h    - Display help information");
        printInfo("  clear        - Clear the screen");
        printInfo("");
        printInfo("Enter content directly to add to the script and execute immediately");
        printInfo("Press Ctrl+C to clear the script content, press Ctrl+D to exit the program");
        printInfo("The variable '_' will save the result of the last execution");
    }

    /**
     * 打印欢迎消息
     */
    private void printWelcome() {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append("Welcome to ", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append("Fluxon Console", AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW))
                .append(" script editor", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));

        terminal.writer().println(asb.toAnsi());
        terminal.writer().println("Enter commands directly to add to the script and execute. Enter :help for more information.");
        terminal.writer().flush();
    }

    /**
     * 打印信息消息
     */
    private void printInfo(String message) {
        terminal.writer().println(message);
        terminal.writer().flush();
    }

    /**
     * 打印错误消息
     */
    private void printError(String message, Throwable err) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(message, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        // 输出错误信息
        if (err != null) {
            asb.append(err.getMessage(), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
            // 输出完整堆栈信息
            terminal.writer().println(asb.toAnsi());
            for (StackTraceElement element : err.getStackTrace()) {
                terminal.writer().println("    at " + element.toString());
            }
            // 如果有更深层的异常，也输出其堆栈
            Throwable cause = err.getCause();
            while (cause != null) {
                terminal.writer().println("Caused by: " + cause);
                for (StackTraceElement element : cause.getStackTrace()) {
                    terminal.writer().println("    at " + element.toString());
                }
                cause = cause.getCause();
            }
        } else {
            terminal.writer().println(asb.toAnsi());
        }
        terminal.writer().flush();
    }

    /**
     * 执行脚本文件
     *
     * @param scriptPath 脚本文件路径
     * @return 执行结果，失败返回 null
     */
    public Object executeFile(Path scriptPath) {
        try {
            String content = new String(Files.readAllBytes(scriptPath), java.nio.charset.StandardCharsets.UTF_8);
            return scriptEngine.eval(content);
        } catch (Exception e) {
            printError("Execution error: ", e);
            return null;
        }
    }

    /**
     * 执行表达式字符串
     *
     * @param expression 表达式
     * @return 执行结果，失败返回 null
     */
    public Object executeExpression(String expression) {
        try {
            return scriptEngine.eval(expression);
        } catch (Exception e) {
            printError("Execution error: ", e);
            return null;
        }
    }

    /**
     * 打印使用说明
     */
    private static void printUsage() {
        System.out.println("Usage: fluxon [options] [script.fs]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -e, --eval <expr>   Evaluate expression and exit");
        System.out.println("  -h, --help          Show this help message");
        System.out.println("  -v, --version       Show version information");
        System.out.println();
        System.out.println("If no arguments are provided, starts interactive REPL mode.");
        System.out.println("If a script file is provided, executes it and exits.");
    }

    /**
     * 打印版本信息
     */
    private static void printVersion() {
        System.out.println("Fluxon Console v1.0.0");
    }

    /**
     * 程序入口
     */
    public static void main(String[] args) {
        try {
            // 无参数：进入交互模式
            if (args.length == 0) {
                FluxonConsole console = new FluxonConsole();
                console.run();
                return;
            }
            // 解析命令行参数
            String firstArg = args[0];
            // 处理帮助选项
            if ("-h".equals(firstArg) || "--help".equals(firstArg)) {
                printUsage();
                return;
            }
            // 处理版本选项
            if ("-v".equals(firstArg) || "--version".equals(firstArg)) {
                printVersion();
                return;
            }
            // 处理表达式执行
            if ("-e".equals(firstArg) || "--eval".equals(firstArg)) {
                if (args.length < 2) {
                    System.err.println("Error: -e/--eval requires an expression argument");
                    System.exit(1);
                    return;
                }
                // 合并后续所有参数作为表达式（支持空格）
                StringBuilder expr = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) expr.append(" ");
                    expr.append(args[i]);
                }
                FluxonConsole console = new FluxonConsole();
                Object result = console.executeExpression(expr.toString());
                if (result != null) {
                    System.out.println(result);
                }
                return;
            }
            // 默认：将第一个参数视为脚本文件路径
            Path scriptPath = Paths.get(firstArg);
            if (!Files.exists(scriptPath)) {
                System.err.println("Error: Script file not found: " + scriptPath);
                System.exit(1);
                return;
            }
            if (!Files.isRegularFile(scriptPath)) {
                System.err.println("Error: Not a regular file: " + scriptPath);
                System.exit(1);
                return;
            }
            FluxonConsole console = new FluxonConsole();
            Object result = console.executeFile(scriptPath);
            if (result != null) {
                System.out.println(result);
            }
        } catch (IOException e) {
            System.err.println("Failed to start: " + e.getMessage());
            System.exit(1);
        }
    }
}