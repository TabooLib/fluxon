package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.MapExpression;
import org.tabooproject.fluxon.runtime.Type;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class MapEvaluator extends ExpressionEvaluator<MapExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.MAP;
    }

    @Override
    public Object evaluate(Interpreter interpreter, MapExpression result) {
        Map<Object, Object> entries = new HashMap<>();
        for (MapExpression.MapEntry entry : result.getEntries()) {
            Object key = interpreter.evaluate(entry.getKey());
            Object value = interpreter.evaluate(entry.getValue());
            entries.put(key, value);
        }
        return entries;
    }

    @Override
    public Type generateBytecode(MapExpression result, CodeContext ctx, MethodVisitor mv) {
        // 创建新的 HashMap 实例
        mv.visitTypeInsn(NEW, HASHMAP.getPath());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, HASHMAP.getPath(), "<init>", "()V", false);
        // 遍历所有 Map 条目，填充 HashMap
        for (MapExpression.MapEntry entry : result.getEntries()) {
            // 复制 Map 引用用于 put 操作
            mv.visitInsn(DUP);
            // 生成 key 的字节码
            Evaluator<ParseResult> keyEval = ctx.getEvaluator(entry.getKey());
            if (keyEval == null) {
                throw new RuntimeException("No evaluator found for key");
            }
            if (keyEval.generateBytecode(entry.getKey(), ctx, mv) == Type.VOID) {
                throw new RuntimeException("Void type is not allowed for map key");
            }
            // 生成 value 的字节码
            Evaluator<ParseResult> valueEval = ctx.getEvaluator(entry.getValue());
            if (valueEval == null) {
                throw new RuntimeException("No evaluator found for value");
            }
            if (valueEval.generateBytecode(entry.getValue(), ctx, mv) == Type.VOID) {
                throw new RuntimeException("Void type is not allowed for map value");
            }
            // 调用 put 方法
            mv.visitMethodInsn(INVOKEINTERFACE, MAP.getPath(), "put", "(" + Type.OBJECT + Type.OBJECT + ")" + Type.OBJECT, true);
            // 丢弃 put 方法的返回值
            mv.visitInsn(POP);
        }
        return Type.OBJECT;
    }

    private static final Type MAP = new Type(Map.class);
    private static final Type HASHMAP = new Type(HashMap.class);
}
