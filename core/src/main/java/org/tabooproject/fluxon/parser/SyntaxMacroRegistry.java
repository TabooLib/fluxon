package org.tabooproject.fluxon.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.macro.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 语法宏注册表
 * <p>
 * 管理语法宏的注册和匹配查找。支持两种使用模式：
 * </p>
 * <ul>
 *   <li><b>静态主注册表</b> - 通过 {@link #primary()} 访问的全局注册表，包含所有内置语法宏</li>
 *   <li><b>独立实例</b> - 通过构造函数创建的独立注册表，用于沙箱隔离或自定义语法集</li>
 * </ul>
 *
 * <h3>使用示例 - 注册自定义语法宏</h3>
 * <pre>{@code
 * // 注册到主注册表
 * SyntaxMacroRegistry.primary().register(new MySyntaxMacro());
 *
 * // 或创建独立注册表
 * SyntaxMacroRegistry registry = SyntaxMacroRegistry.withDefaults();
 * registry.register(new MySyntaxMacro());
 *
 * CompilationContext context = new CompilationContext(source);
 * context.setSyntaxMacroRegistry(registry);
 * }</pre>
 *
 * <h3>匹配规则</h3>
 * <p>
 * 当解析器需要解析 primary 表达式时，会按优先级从高到低依次尝试每个语法宏的 {@code matches()} 方法。
 * 第一个返回 true 的语法宏将被用于解析。
 * </p>
 *
 * <h3>线程安全</h3>
 * <p>
 * 注册表在注册阶段（通常在应用启动时）应在单线程环境下操作。
 * 注册完成后，多个线程可以并发调用 {@link #findMatch} 方法进行只读访问。
 * </p>
 *
 * @see SyntaxMacro
 */
public class SyntaxMacroRegistry {

    /**
     * 静态主注册表实例
     */
    private static final SyntaxMacroRegistry PRIMARY = createDefault();

    /**
     * 按优先级降序排列的语法宏列表
     */
    private final List<SyntaxMacro> macros;

    /**
     * 是否需要重新排序
     */
    private boolean needsSort = false;

    /**
     * 获取静态主注册表
     * <p>
     * 这是全局默认的语法宏注册表，包含所有内置语法宏。
     * 如果 {@link org.tabooproject.fluxon.compiler.CompilationContext} 未显式设置注册表，
     * 将自动使用此主注册表。
     * </p>
     *
     * @return 主注册表实例
     */
    @NotNull
    public static SyntaxMacroRegistry primary() {
        return PRIMARY;
    }

    /**
     * 创建一个包含所有内置语法宏的新注册表
     * <p>
     * 用于需要在内置语法基础上添加自定义语法的场景。
     * </p>
     *
     * @return 包含内置语法宏的新注册表
     */
    @NotNull
    public static SyntaxMacroRegistry withDefaults() {
        return createDefault();
    }

    /**
     * 创建空的语法宏注册表
     */
    public SyntaxMacroRegistry() {
        this.macros = new CopyOnWriteArrayList<>();
    }

    /**
     * 创建包含所有内置语法宏的默认注册表
     */
    private static SyntaxMacroRegistry createDefault() {
        SyntaxMacroRegistry registry = new SyntaxMacroRegistry();
        registerBuiltinMacros(registry);
        return registry;
    }

    /**
     * 注册所有内置语法宏
     */
    private static void registerBuiltinMacros(SyntaxMacroRegistry registry) {
        // 扩展语法 (高优先级)
        // Command (priority: 1000) - 需要在标识符之前匹配
        registry.register(new CommandSyntaxMacro());
        // Domain (priority: 1000) - 需要在标识符之前匹配
        registry.register(new DomainSyntaxMacro());
        // 解构赋值 (priority: 900) - 需要在分组表达式之前匹配
        registry.register(new DestructuringSyntaxMacro());

        // 控制流关键字 (priority: 100)
        registry.register(new IfSyntaxMacro());
        registry.register(new ForSyntaxMacro());
        registry.register(new WhileSyntaxMacro());
        registry.register(new TrySyntaxMacro());
        registry.register(new WhenSyntaxMacro());
        registry.register(new NewSyntaxMacro());
        registry.register(new ImplMacro());
        registry.register(new StaticSyntaxMacro());

        // Lambda (priority: 100)
        registry.register(new LambdaSyntaxMacro());

        // 复合结构 (priority: 80)
        registry.register(new BlockSyntaxMacro());
        registry.register(new ListSyntaxMacro());

        // 分组表达式 (priority: 50)
        registry.register(new GroupingSyntaxMacro());

        // 字面量 (priority: 30)
        registry.register(new LiteralSyntaxMacro());

        // 标识符 (priority: 10) - 最低优先级，作为兜底
        registry.register(new IdentifierSyntaxMacro());
    }

    /**
     * 注册语法宏
     * <p>
     * 语法宏会按优先级排序，优先级高的先尝试匹配。
     * 如果已存在相同优先级的宏，新注册的宏会排在后面。
     * </p>
     *
     * @param macro 要注册的语法宏
     */
    public void register(@NotNull SyntaxMacro macro) {
        macros.add(macro);
        needsSort = true;
    }

    /**
     * 查找匹配当前位置的语法宏
     * <p>
     * 按优先级从高到低依次检查每个语法宏，返回第一个匹配的。
     * </p>
     *
     * @param parser 解析器实例
     * @return 匹配的语法宏，如果没有匹配则返回 null
     */
    @Nullable
    public SyntaxMacro findMatch(@NotNull Parser parser) {
        ensureSorted();
        for (SyntaxMacro macro : macros) {
            if (macro.matches(parser)) {
                return macro;
            }
        }
        return null;
    }

    /**
     * 获取所有已注册的语法宏
     *
     * @return 语法宏列表（按优先级降序）
     */
    @NotNull
    public List<SyntaxMacro> getMacros() {
        ensureSorted();
        return new ArrayList<>(macros);
    }

    /**
     * 确保列表已按优先级排序
     */
    private void ensureSorted() {
        if (needsSort) {
            macros.sort(Comparator.comparingInt(SyntaxMacro::priority).reversed());
            needsSort = false;
        }
    }

    /**
     * 清空所有已注册的语法宏
     * <p>
     * 警告：此操作不可逆，主要用于测试环境。
     * </p>
     */
    public void clear() {
        macros.clear();
        needsSort = false;
    }
}
