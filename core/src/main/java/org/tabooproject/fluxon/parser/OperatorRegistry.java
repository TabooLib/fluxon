package org.tabooproject.fluxon.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.operator.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 运算符注册表
 * <p>
 * 管理中缀运算符和前缀运算符的注册和匹配查找。
 * 支持静态主注册表和独立实例两种使用模式。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 注册自定义运算符到主注册表
 * OperatorRegistry.primary().registerInfix(new MyInfixOperator());
 *
 * // 或创建独立注册表
 * OperatorRegistry registry = OperatorRegistry.withDefaults();
 * registry.registerInfix(new MyInfixOperator());
 * }</pre>
 *
 * @see InfixOperator
 * @see PrefixOperator
 */
public class OperatorRegistry {

    /**
     * 静态主注册表实例
     */
    private static final OperatorRegistry PRIMARY = createDefault();

    /**
     * 中缀运算符列表（按绑定力降序排列用于快速查找）
     */
    private final List<InfixOperator> infixOperators;

    /**
     * 前缀运算符列表（按优先级降序排列）
     */
    private final List<PrefixOperator> prefixOperators;

    private boolean infixNeedsSort = false;
    private boolean prefixNeedsSort = false;

    /**
     * 获取静态主注册表
     */
    @NotNull
    public static OperatorRegistry primary() {
        return PRIMARY;
    }

    /**
     * 创建包含所有内置运算符的新注册表
     */
    @NotNull
    public static OperatorRegistry withDefaults() {
        return createDefault();
    }

    /**
     * 创建空的运算符注册表
     */
    public OperatorRegistry() {
        this.infixOperators = new ArrayList<>();
        this.prefixOperators = new ArrayList<>();
    }

    /**
     * 创建包含所有内置运算符的默认注册表
     */
    private static OperatorRegistry createDefault() {
        OperatorRegistry registry = new OperatorRegistry();
        registerBuiltinOperators(registry);
        return registry;
    }

    /**
     * 注册所有内置运算符
     */
    private static void registerBuiltinOperators(OperatorRegistry registry) {
        // === 中缀运算符（按绑定力从低到高） ===

        // Assignment (10) - 右结合
        registry.registerInfix(new AssignmentInfixOperator());
        // Elvis (20) - 右结合
        registry.registerInfix(new ElvisInfixOperator());
        // Ternary (30) - 右结合
        registry.registerInfix(new TernaryInfixOperator());
        // LogicalOr (40) - 左结合
        registry.registerInfix(new LogicalOrInfixOperator());
        // LogicalAnd (50) - 左结合
        registry.registerInfix(new LogicalAndInfixOperator());
        // Range (60) - 非结合
        registry.registerInfix(new RangeInfixOperator());
        // Equality (70) - 左结合
        registry.registerInfix(new EqualityInfixOperator());
        // Comparison (80) - 左结合
        registry.registerInfix(new ComparisonInfixOperator());
        // Term (90) - 左结合
        registry.registerInfix(new TermInfixOperator());
        // Factor (100) - 左结合
        registry.registerInfix(new FactorInfixOperator());
        // ContextCall (110) - 左结合
        registry.registerInfix(new ContextCallInfixOperator());

        // === 前缀运算符 ===

        // Reference (100) - &, &?
        registry.registerPrefix(new ReferencePrefixOperator());
        // Unary (105) - !, -
        registry.registerPrefix(new UnaryPrefixOperator());
        // Await (105)
        registry.registerPrefix(new AwaitPrefixOperator());
    }

    /**
     * 注册中缀运算符
     */
    public void registerInfix(@NotNull InfixOperator operator) {
        infixOperators.add(operator);
        infixNeedsSort = true;
    }

    /**
     * 注册前缀运算符
     */
    public void registerPrefix(@NotNull PrefixOperator operator) {
        prefixOperators.add(operator);
        prefixNeedsSort = true;
    }

    /**
     * 查找匹配当前位置且绑定力 >= minBindingPower 的中缀运算符
     *
     * @param parser          解析器实例
     * @param minBindingPower 最小绑定力
     * @return 匹配的中缀运算符，如果没有匹配则返回 null
     */
    @Nullable
    public InfixOperator findInfixMatch(@NotNull Parser parser, int minBindingPower) {
        ensureInfixSorted();
        for (InfixOperator op : infixOperators) {
            if (op.bindingPower() >= minBindingPower && op.matches(parser)) {
                return op;
            }
        }
        return null;
    }

    /**
     * 查找匹配当前位置的前缀运算符
     *
     * @param parser 解析器实例
     * @return 匹配的前缀运算符，如果没有匹配则返回 null
     */
    @Nullable
    public PrefixOperator findPrefixMatch(@NotNull Parser parser) {
        ensurePrefixSorted();
        for (PrefixOperator op : prefixOperators) {
            if (op.matches(parser)) {
                return op;
            }
        }
        return null;
    }

    private void ensureInfixSorted() {
        if (infixNeedsSort) {
            // 按绑定力降序排列（高优先级先检查）
            infixOperators.sort(Comparator.comparingInt(InfixOperator::bindingPower).reversed());
            infixNeedsSort = false;
        }
    }

    private void ensurePrefixSorted() {
        if (prefixNeedsSort) {
            prefixOperators.sort(Comparator.comparingInt(PrefixOperator::priority).reversed());
            prefixNeedsSort = false;
        }
    }

    /**
     * 获取所有已注册的中缀运算符
     */
    @NotNull
    public List<InfixOperator> getInfixOperators() {
        ensureInfixSorted();
        return new ArrayList<>(infixOperators);
    }

    /**
     * 获取所有已注册的前缀运算符
     */
    @NotNull
    public List<PrefixOperator> getPrefixOperators() {
        ensurePrefixSorted();
        return new ArrayList<>(prefixOperators);
    }
}
