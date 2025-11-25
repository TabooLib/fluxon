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
import org.tabooproject.fluxon.parser.expression.ListExpression;
import org.tabooproject.fluxon.runtime.Type;

import org.tabooproject.fluxon.runtime.collection.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class ListEvaluator extends ExpressionEvaluator<ListExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.LIST;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ListExpression result) {
        int size = result.getElements().size();
        if (result.isImmutable()) {
            if (size == 0) {
                return ImmutableList.empty();
            }
            Object[] values = new Object[size];
            int i = 0;
            for (ParseResult element : result.getElements()) {
                values[i++] = interpreter.evaluate(element);
            }
            return ImmutableList.of(values);
        }
        List<Object> elements = new ArrayList<>(size);
        for (ParseResult element : result.getElements()) {
            elements.add(interpreter.evaluate(element));
        }
        return elements;
    }

    @Override
    public Type generateBytecode(ListExpression result, CodeContext ctx, MethodVisitor mv) {
        if (result.isImmutable()) {
            generateImmutableListBytecode(result, ctx, mv);
        } else {
            generateMutableListBytecode(result, ctx, mv);
        }
        return Type.OBJECT;
    }

    private void generateMutableListBytecode(ListExpression result, CodeContext ctx, MethodVisitor mv) {
        mv.visitTypeInsn(NEW, ARRAY_LIST.getPath());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, ARRAY_LIST.getPath(), "<init>", "()V", false);
        for (ParseResult element : result.getElements()) {
            mv.visitInsn(DUP);
            emitElement(ctx, mv, element);
            mv.visitMethodInsn(INVOKEVIRTUAL, ARRAY_LIST.getPath(), "add", "(" + Type.OBJECT + ")Z", false);
            mv.visitInsn(POP);
        }
    }

    private void generateImmutableListBytecode(ListExpression result, CodeContext ctx, MethodVisitor mv) {
        List<ParseResult> elements = result.getElements();
        mv.visitLdcInsn(elements.size());
        mv.visitTypeInsn(ANEWARRAY, Type.OBJECT.getPath());
        for (int i = 0; i < elements.size(); i++) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(i);
            emitElement(ctx, mv, elements.get(i));
            mv.visitInsn(AASTORE);
        }
        mv.visitMethodInsn(INVOKESTATIC, IMMUTABLE_LIST.getPath(), "of", "(" + OBJECT_ARRAY + ")" + IMMUTABLE_LIST, false);
    }

    private void emitElement(CodeContext ctx, MethodVisitor mv, ParseResult element) {
        Evaluator<ParseResult> eval = ctx.getEvaluator(element);
        if (eval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for element");
        }
        if (eval.generateBytecode(element, ctx, mv) == Type.VOID) {
            throw new VoidError("Void type is not allowed for list element");
        }
    }

    private static final Type ARRAY_LIST = new Type(ArrayList.class);
    private static final Type OBJECT_ARRAY = new Type(Object.class, 1);
    private static final Type IMMUTABLE_LIST = new Type(ImmutableList.class);
}
