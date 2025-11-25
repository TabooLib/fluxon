package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.MapExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.collection.ImmutableMap;

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
        int size = result.getEntries().size();
        if (result.isImmutable()) {
            if (size == 0) {
                return ImmutableMap.empty();
            }
            Object[] keys = new Object[size];
            Object[] values = new Object[size];
            int i = 0;
            for (MapExpression.MapEntry entry : result.getEntries()) {
                keys[i] = interpreter.evaluate(entry.getKey());
                values[i] = interpreter.evaluate(entry.getValue());
                i++;
            }
            return ImmutableMap.of(keys, values);
        }
        Map<Object, Object> entries = new HashMap<>(Math.max(4, (int) (size / 0.75f) + 1));
        for (MapExpression.MapEntry entry : result.getEntries()) {
            Object key = interpreter.evaluate(entry.getKey());
            Object value = interpreter.evaluate(entry.getValue());
            entries.put(key, value);
        }
        return entries;
    }

    @Override
    public Type generateBytecode(MapExpression result, CodeContext ctx, MethodVisitor mv) {
        if (result.isImmutable()) {
            generateImmutableMapBytecode(result, ctx, mv);
        } else {
            generateMutableMapBytecode(result, ctx, mv);
        }
        return Type.OBJECT;
    }

    private void generateMutableMapBytecode(MapExpression result, CodeContext ctx, MethodVisitor mv) {
        int size = result.getEntries().size();
        int capacity = Math.max(4, (int) (size / 0.75f) + 1);
        // 创建新的 HashMap(capacity) 实例
        mv.visitTypeInsn(NEW, HASHMAP.getPath());
        mv.visitInsn(DUP);
        mv.visitLdcInsn(capacity);
        mv.visitMethodInsn(INVOKESPECIAL, HASHMAP.getPath(), "<init>", "(" + Type.I + ")V", false);
        // 遍历所有 Map 条目，填充 HashMap
        for (MapExpression.MapEntry entry : result.getEntries()) {
            mv.visitInsn(DUP);
            emitKey(ctx, mv, entry.getKey());
            emitValue(ctx, mv, entry.getValue());
            mv.visitMethodInsn(INVOKEINTERFACE, MAP.getPath(), "put", "(" + Type.OBJECT + Type.OBJECT + ")" + Type.OBJECT, true);
            mv.visitInsn(POP);
        }
    }

    private void generateImmutableMapBytecode(MapExpression result, CodeContext ctx, MethodVisitor mv) {
        int size = result.getEntries().size();
        if (size == 0) {
            mv.visitMethodInsn(INVOKESTATIC, IMMUTABLE_MAP.getPath(), "empty", "()" + IMMUTABLE_MAP, false);
            return;
        }
        // keys array
        mv.visitLdcInsn(size);
        mv.visitTypeInsn(ANEWARRAY, Type.OBJECT.getPath());
        for (int i = 0; i < size; i++) {
            MapExpression.MapEntry entry = result.getEntries().get(i);
            mv.visitInsn(DUP);
            mv.visitLdcInsn(i);
            emitKey(ctx, mv, entry.getKey());
            mv.visitInsn(AASTORE);
        }
        // values array
        mv.visitLdcInsn(size);
        mv.visitTypeInsn(ANEWARRAY, Type.OBJECT.getPath());
        for (int i = 0; i < size; i++) {
            MapExpression.MapEntry entry = result.getEntries().get(i);
            mv.visitInsn(DUP);
            mv.visitLdcInsn(i);
            emitValue(ctx, mv, entry.getValue());
            mv.visitInsn(AASTORE);
        }
        mv.visitMethodInsn(INVOKESTATIC, IMMUTABLE_MAP.getPath(), "of", "(" + OBJECT_ARRAY + OBJECT_ARRAY + ")" + IMMUTABLE_MAP, false);
    }

    private void emitKey(CodeContext ctx, MethodVisitor mv, ParseResult key) {
        Evaluator<ParseResult> keyEval = ctx.getEvaluator(key);
        if (keyEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for key");
        }
        if (keyEval.generateBytecode(key, ctx, mv) == Type.VOID) {
            throw new VoidError("Void type is not allowed for map key");
        }
    }

    private void emitValue(CodeContext ctx, MethodVisitor mv, ParseResult value) {
        Evaluator<ParseResult> valueEval = ctx.getEvaluator(value);
        if (valueEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for value");
        }
        if (valueEval.generateBytecode(value, ctx, mv) == Type.VOID) {
            throw new VoidError("Void type is not allowed for map value");
        }
    }

    private static final Type MAP = new Type(Map.class);
    private static final Type HASHMAP = new Type(HashMap.class);
    private static final Type IMMUTABLE_MAP = new Type(ImmutableMap.class);
    private static final Type OBJECT_ARRAY = new Type(Object.class, 1);
}
