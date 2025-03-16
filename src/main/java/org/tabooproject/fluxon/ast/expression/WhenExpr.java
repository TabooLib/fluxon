package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

import java.util.List;

/**
 * when 表达式节点
 * 表示 when 表达式
 */
public class WhenExpr extends Expr {

    private final Expr subject; // when 的主体表达式，可能为 null（表示无主体的 when）
    private final List<WhenBranch> branches;
    private final Expr elseBranch;

    /**
     * 创建 when 表达式节点
     *
     * @param subject when 的主体表达式
     * @param branches 分支列表
     * @param elseBranch else 分支
     * @param location 源代码位置
     */
    public WhenExpr(Expr subject, List<WhenBranch> branches, Expr elseBranch, SourceLocation location) {
        super(location);
        this.subject = subject;
        this.branches = branches;
        this.elseBranch = elseBranch;
    }

    /**
     * 获取 when 的主体表达式
     *
     * @return 主体表达式，可能为 null
     */
    public Expr getSubject() {
        return subject;
    }

    /**
     * 获取分支列表
     *
     * @return 分支列表
     */
    public List<WhenBranch> getBranches() {
        return branches;
    }

    /**
     * 获取 else 分支
     *
     * @return else 分支
     */
    public Expr getElseBranch() {
        return elseBranch;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitWhenExpr(this);
    }

    @Override
    public String toString() {
        return "when " + subject + " {\n" +
                branches.stream().map(branch -> "    " + branch.getCondition() + " -> " + branch.getBody() + "\n").reduce("", String::concat) +
                (elseBranch != null ? "    else -> " + elseBranch + "\n" : "") +
                "}";
    }

    /**
     * when 分支
     */
    public static class WhenBranch {
        private final Expr condition;
        private final Expr body;

        /**
         * 创建 when 分支
         *
         * @param condition 条件表达式
         * @param body 分支体
         */
        public WhenBranch(Expr condition, Expr body) {
            this.condition = condition;
            this.body = body;
        }

        /**
         * 获取条件表达式
         *
         * @return 条件表达式
         */
        public Expr getCondition() {
            return condition;
        }

        /**
         * 获取分支体
         *
         * @return 分支体
         */
        public Expr getBody() {
            return body;
        }
    }
}