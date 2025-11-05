package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.error.EvaluatorNotFoundException;
import org.tabooproject.fluxon.interpreter.error.VoidValueException;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.ListExpression;
import org.tabooproject.fluxon.runtime.Type;

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
        List<Object> elements = new ArrayList<>(result.getElements().size());
        for (ParseResult element : result.getElements()) {
            elements.add(interpreter.evaluate(element));
        }
        return elements;
    }

    @Override
    public Type generateBytecode(ListExpression result, CodeContext ctx, MethodVisitor mv) {
        // 创建 ArrayList
        mv.visitTypeInsn(NEW, ARRAY_LIST.getPath());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, ARRAY_LIST.getPath(), "<init>", "()V", false);
        // 遍历所有元素，添加到 ArrayList
        for (ParseResult element : result.getElements()) {
            // 复制 ArrayList 引用
            mv.visitInsn(DUP);
            // 生成元素的字节码
            Evaluator<ParseResult> eval = ctx.getEvaluator(element);
            if (eval == null) {
                throw new EvaluatorNotFoundException("No evaluator found for element");
            }
            if (eval.generateBytecode(element, ctx, mv) == Type.VOID) {
                throw new VoidValueException("Void type is not allowed for list element");
            }
            // 调用 ArrayList.add
            mv.visitMethodInsn(INVOKEVIRTUAL, ARRAY_LIST.getPath(), "add", "(" + Type.OBJECT + ")Z", false);
            // 丢弃 add 方法的返回值 (boolean)
            mv.visitInsn(POP);
        }
        return Type.OBJECT;
    }

    private static final Type ARRAY_LIST = new Type(ArrayList.class);
}
