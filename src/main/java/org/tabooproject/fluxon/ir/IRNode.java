package org.tabooproject.fluxon.ir;

/**
 * IR 节点接口
 * 表示中间表示中的节点
 */
public interface IRNode {
    
    /**
     * 接受访问者
     * 
     * @param visitor 访问者
     * @param <T> 返回类型
     * @return 访问结果
     */
    <T> T accept(IRVisitor<T> visitor);
}