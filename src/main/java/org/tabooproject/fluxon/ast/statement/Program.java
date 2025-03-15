package org.tabooproject.fluxon.ast.statement;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

import java.util.List;

/**
 * 程序节点
 * 表示整个程序，作为 AST 的根节点
 */
public class Program extends Stmt {
    private final List<Stmt> statements;
    private final boolean strictMode;
    
    /**
     * 创建程序节点
     * 
     * @param statements 语句列表
     * @param strictMode 是否为严格模式
     * @param location 源代码位置
     */
    public Program(List<Stmt> statements, boolean strictMode, SourceLocation location) {
        super(location);
        this.statements = statements;
        this.strictMode = strictMode;
    }
    
    /**
     * 获取语句列表
     * 
     * @return 语句列表
     */
    public List<Stmt> getStatements() {
        return statements;
    }
    
    /**
     * 检查是否为严格模式
     * 
     * @return 是否为严格模式
     */
    public boolean isStrictMode() {
        return strictMode;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitProgram(this);
    }
}