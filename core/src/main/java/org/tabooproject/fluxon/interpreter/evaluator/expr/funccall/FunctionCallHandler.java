package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.Type;

/**
 * 函数调用策略处理器
 * <p>
 * 采用策略模式分离不同场景下的函数调用处理逻辑。设计目的：
 * <ul>
 *   <li>分离关注点：编译时已知类型 vs 运行时动态类型</li>
 *   <li>避免运行时分支：根据编译时信息选择最优路径</li>
 *   <li>支持延迟解析：参数类型未知时延迟到运行时选择重载</li>
 * </ul>
 * <p>
 * 实现类说明：
 * <ul>
 *   <li>{@link ResolvedExtensionHandler} - 扩展函数已在编译时完全解析，直接调用</li>
 *   <li>{@link DynamicResolutionHandler} - 通用动态解析，根据函数名和参数数量在运行时查找</li>
 *   <li>{@link DeferredOverloadHandler} - 系统函数有多个重载且参数类型未知，运行时根据实际参数类型选择重载</li>
 *   <li>{@link DeferredExtensionHandler} - 扩展函数有多个重载且参数类型未知，运行时根据实际参数类型选择重载</li>
 *   <li>{@link CachedDeferredHandler} - 延迟解析后缓存结果，后续调用复用已解析的函数</li>
 * </ul>
 * <p>
 * 选择逻辑（优先级从高到低）：
 * <ol>
 *   <li>扩展函数已解析 → ResolvedExtensionHandler</li>
 *   <li>扩展函数有多重载且参数类型未知 → DeferredExtensionHandler</li>
 *   <li>系统函数有多重载且参数类型未知 → DeferredOverloadHandler</li>
 *   <li>已缓存延迟解析结果 → CachedDeferredHandler</li>
 *   <li>其他情况 → DynamicResolutionHandler</li>
 * </ol>
 *
 * @author sky
 */
public interface FunctionCallHandler {

    /**
     * 解释执行：准备函数调用上下文
     */
    FunctionContext<?> prepareCall(Interpreter interpreter, FunctionCallExpression expr, int argCount);

    /**
     * 解释执行：完成函数调用并返回结果
     */
    Type finishCall(Interpreter interpreter, FunctionCallExpression expr, FunctionContext<?> ctx);

    /**
     * 字节码生成：生成 prepareCall 调用
     */
    PrepareCallResult generatePrepareCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, int argCount);

    /**
     * 字节码生成：生成 finishCall 调用
     */
    Type generateFinishCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, PrepareCallResult prepareResult, Type returnType);

    /**
     * prepareCall 生成结果
     */
    class PrepareCallResult {
        public final int ctxSlot;
        public final int deferredSlot;

        public PrepareCallResult(int ctxSlot) {
            this(ctxSlot, -1);
        }

        public PrepareCallResult(int ctxSlot, int deferredSlot) {
            this.ctxSlot = ctxSlot;
            this.deferredSlot = deferredSlot;
        }
    }
}
