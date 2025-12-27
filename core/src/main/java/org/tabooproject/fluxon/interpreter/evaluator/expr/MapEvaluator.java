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
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class MapEvaluator extends ExpressionEvaluator<MapExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.MAP;
    }

    @Override
    public Object evaluate(Interpreter interpreter, MapExpression result) {
        List<MapExpression.MapEntry> entries = result.getEntries();
        int size = entries.size();
        if (result.isImmutable()) {
            // 使用 inline 工厂方法避免数组分配
            switch (size) {
                case 0: return ImmutableMap.empty();
                case 1: return ImmutableMap.of(
                        interpreter.evaluate(entries.get(0).getKey()),
                        interpreter.evaluate(entries.get(0).getValue()));
                case 2: return ImmutableMap.of(
                        interpreter.evaluate(entries.get(0).getKey()),
                        interpreter.evaluate(entries.get(0).getValue()),
                        interpreter.evaluate(entries.get(1).getKey()),
                        interpreter.evaluate(entries.get(1).getValue()));
                case 3: return ImmutableMap.of(
                        interpreter.evaluate(entries.get(0).getKey()),
                        interpreter.evaluate(entries.get(0).getValue()),
                        interpreter.evaluate(entries.get(1).getKey()),
                        interpreter.evaluate(entries.get(1).getValue()),
                        interpreter.evaluate(entries.get(2).getKey()),
                        interpreter.evaluate(entries.get(2).getValue()));
                case 4: return ImmutableMap.of(
                        interpreter.evaluate(entries.get(0).getKey()),
                        interpreter.evaluate(entries.get(0).getValue()),
                        interpreter.evaluate(entries.get(1).getKey()),
                        interpreter.evaluate(entries.get(1).getValue()),
                        interpreter.evaluate(entries.get(2).getKey()),
                        interpreter.evaluate(entries.get(2).getValue()),
                        interpreter.evaluate(entries.get(3).getKey()),
                        interpreter.evaluate(entries.get(3).getValue()));
                default:
                    // >4 条目回退到数组模式
                    Object[] keys = new Object[size];
                    Object[] values = new Object[size];
                    for (int i = 0; i < size; i++) {
                        keys[i] = interpreter.evaluate(entries.get(i).getKey());
                        values[i] = interpreter.evaluate(entries.get(i).getValue());
                    }
                    return ImmutableMap.of(keys, values);
            }
        }
        Map<Object, Object> map = new HashMap<>(Math.max(4, (int) (size / 0.75f) + 1));
        for (MapExpression.MapEntry entry : entries) {
            Object key = interpreter.evaluate(entry.getKey());
            Object value = interpreter.evaluate(entry.getValue());
            map.put(key, value);
        }
        return map;
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
        List<MapExpression.MapEntry> entries = result.getEntries();
        int size = entries.size();
        if (size == 0) {
            mv.visitMethodInsn(INVOKESTATIC, IMMUTABLE_MAP.getPath(), "empty", "()" + IMMUTABLE_MAP, false);
            return;
        }
        if (size <= 4) {
            // 使用 inline 工厂方法，避免数组分配
            for (MapExpression.MapEntry entry : entries) {
                emitKey(ctx, mv, entry.getKey());
                emitValue(ctx, mv, entry.getValue());
            }
            // 构建方法描述符: (k1, v1, k2, v2, ...) -> ImmutableMap
            StringBuilder desc = new StringBuilder("(");
            for (int i = 0; i < size * 2; i++) {
                desc.append(Type.OBJECT);
            }
            desc.append(")").append(IMMUTABLE_MAP);
            mv.visitMethodInsn(INVOKESTATIC, IMMUTABLE_MAP.getPath(), "of", desc.toString(), false);
            return;
        }
        // >4 条目回退到数组模式
        // keys array
        mv.visitLdcInsn(size);
        mv.visitTypeInsn(ANEWARRAY, Type.OBJECT.getPath());
        for (int i = 0; i < size; i++) {
            MapExpression.MapEntry entry = entries.get(i);
            mv.visitInsn(DUP);
            mv.visitLdcInsn(i);
            emitKey(ctx, mv, entry.getKey());
            mv.visitInsn(AASTORE);
        }
        // values array
        mv.visitLdcInsn(size);
        mv.visitTypeInsn(ANEWARRAY, Type.OBJECT.getPath());
        for (int i = 0; i < size; i++) {
            MapExpression.MapEntry entry = entries.get(i);
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
