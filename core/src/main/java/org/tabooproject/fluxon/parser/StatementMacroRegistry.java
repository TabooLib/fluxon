package org.tabooproject.fluxon.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.macro.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 语句宏注册表
 * <p>
 * 管理语句宏的注册和匹配查找。
 *
 * @see StatementMacro
 */
public class StatementMacroRegistry {

    private static final StatementMacroRegistry PRIMARY = createDefault();

    private final List<StatementMacro> macros;
    private boolean needsSort = false;

    @NotNull
    public static StatementMacroRegistry primary() {
        return PRIMARY;
    }

    @NotNull
    public static StatementMacroRegistry withDefaults() {
        return createDefault();
    }

    public StatementMacroRegistry() {
        this.macros = new ArrayList<>();
    }

    private static StatementMacroRegistry createDefault() {
        StatementMacroRegistry registry = new StatementMacroRegistry();
        registerBuiltinMacros(registry);
        return registry;
    }

    private static void registerBuiltinMacros(StatementMacroRegistry registry) {
        // 函数定义 (priority: 200) - 顶层
        registry.register(new FunctionDefinitionMacro());

        // 控制流语句 (priority: 100)
        registry.register(new ReturnStatementMacro());
        registry.register(new BreakStatementMacro());
        registry.register(new ContinueStatementMacro());

        // 表达式语句 (priority: 0) - 兜底
        registry.register(new ExpressionStatementMacro());
    }

    public void register(@NotNull StatementMacro macro) {
        macros.add(macro);
        needsSort = true;
    }

    @Nullable
    public StatementMacro findTopLevelMatch(@NotNull Parser parser) {
        ensureSorted();
        for (StatementMacro macro : macros) {
            if (macro.matchesTopLevel(parser)) {
                return macro;
            }
        }
        return null;
    }

    @Nullable
    public StatementMacro findSubMatch(@NotNull Parser parser) {
        ensureSorted();
        for (StatementMacro macro : macros) {
            if (macro.matchesSub(parser)) {
                return macro;
            }
        }
        return null;
    }

    @NotNull
    public List<StatementMacro> getMacros() {
        ensureSorted();
        return new ArrayList<>(macros);
    }

    private void ensureSorted() {
        if (needsSort) {
            macros.sort(Comparator.comparingInt(StatementMacro::priority).reversed());
            needsSort = false;
        }
    }

    public void clear() {
        macros.clear();
        needsSort = false;
    }
}
