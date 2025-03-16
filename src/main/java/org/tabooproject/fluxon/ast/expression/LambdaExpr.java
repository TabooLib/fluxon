package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

import java.util.List;

/**
 * Lambda 表达式节点
 * 表示 Lambda 表达式，如 { x -> x * x }
 */
public class LambdaExpr extends Expr {
    private final List<Parameter> parameters;
    private final Expr body;
    
    /**
     * 创建 Lambda 表达式节点
     * 
     * @param parameters 参数列表
     * @param body 函数体
     * @param location 源代码位置
     */
    public LambdaExpr(List<Parameter> parameters, Expr body, SourceLocation location) {
        super(location);
        this.parameters = parameters;
        this.body = body;
    }
    
    /**
     * 获取参数列表
     * 
     * @return 参数列表
     */
    public List<Parameter> getParameters() {
        return parameters;
    }
    
    /**
     * 获取函数体
     * 
     * @return 函数体
     */
    public Expr getBody() {
        return body;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitLambdaExpr(this);
    }

    @Override
    public String toString() {
        return "{ " + parameters.toString() + " -> " + body.toString() + " }";
    }

    /**
     * Lambda 参数
     */
    public static class Parameter {
        private final String name;
        private final String type; // 类型注解，可能为 null
        
        /**
         * 创建 Lambda 参数
         * 
         * @param name 参数名
         * @param type 类型注解
         */
        public Parameter(String name, String type) {
            this.name = name;
            this.type = type;
        }
        
        /**
         * 获取参数名
         * 
         * @return 参数名
         */
        public String getName() {
            return name;
        }
        
        /**
         * 获取类型注解
         * 
         * @return 类型注解，可能为 null
         */
        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return name + ": " + type;
        }
    }
}