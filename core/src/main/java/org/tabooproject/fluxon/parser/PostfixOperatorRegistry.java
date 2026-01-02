package org.tabooproject.fluxon.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.operator.CallPostfixOperator;
import org.tabooproject.fluxon.parser.operator.IndexAccessPostfixOperator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 后缀运算符注册表
 * <p>
 * 管理后缀运算符的注册和匹配查找。
 *
 * @see PostfixOperator
 */
public class PostfixOperatorRegistry {

    private static final PostfixOperatorRegistry PRIMARY = createDefault();

    private final List<PostfixOperator> operators;
    private boolean needsSort = false;

    @NotNull
    public static PostfixOperatorRegistry primary() {
        return PRIMARY;
    }

    @NotNull
    public static PostfixOperatorRegistry withDefaults() {
        return createDefault();
    }

    public PostfixOperatorRegistry() {
        this.operators = new CopyOnWriteArrayList<>();
    }

    private static PostfixOperatorRegistry createDefault() {
        PostfixOperatorRegistry registry = new PostfixOperatorRegistry();
        registerBuiltinOperators(registry);
        return registry;
    }

    private static void registerBuiltinOperators(PostfixOperatorRegistry registry) {
        // 索引访问 []
        registry.register(new IndexAccessPostfixOperator());
        // 后缀函数调用 ()
        registry.register(new CallPostfixOperator());
    }

    public void register(@NotNull PostfixOperator operator) {
        operators.add(operator);
        needsSort = true;
    }

    /**
     * 查找匹配的后缀运算符
     *
     * @param parser 解析器
     * @param expr   当前表达式
     * @return 匹配的运算符，如果没有匹配则返回 null
     */
    @Nullable
    public PostfixOperator findMatch(@NotNull Parser parser, @NotNull ParseResult expr) {
        ensureSorted();
        for (PostfixOperator op : operators) {
            if (op.matches(parser, expr)) {
                return op;
            }
        }
        return null;
    }

    private void ensureSorted() {
        if (needsSort) {
            operators.sort(Comparator.comparingInt(PostfixOperator::priority).reversed());
            needsSort = false;
        }
    }

    public void clear() {
        operators.clear();
        needsSort = false;
    }
}
