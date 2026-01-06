package org.tabooproject.fluxon.interpreter.bytecode.emitter;

import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * 类生成结果
 */
public class EmitResult {

    private final byte[] bytecode;
    private final List<LambdaFunctionDefinition> lambdaDefinitions;
    private final CodeContext ctx;

    /**
     * 构造仅包含字节码的结果（用于不产生 Lambda 的生成器）
     */
    public EmitResult(byte[] bytecode) {
        this(bytecode, null, null);
    }

    public EmitResult(byte[] bytecode, List<LambdaFunctionDefinition> lambdaDefinitions) {
        this(bytecode, lambdaDefinitions, null);
    }

    public EmitResult(byte[] bytecode, List<LambdaFunctionDefinition> lambdaDefinitions, CodeContext ctx) {
        this.bytecode = bytecode;
        this.lambdaDefinitions = lambdaDefinitions != null ? lambdaDefinitions : new ArrayList<>();
        this.ctx = ctx;
    }

    public byte[] getBytecode() {
        return bytecode;
    }

    public List<LambdaFunctionDefinition> getLambdaDefinitions() {
        return lambdaDefinitions;
    }

    public CodeContext getCtx() {
        return ctx;
    }
}
