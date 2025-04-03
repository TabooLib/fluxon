package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
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
        List<Object> elements = new ArrayList<>();
        for (ParseResult element : result.getElements()) {
            elements.add(interpreter.evaluate(element));
        }
        return elements;
    }

    @Override
    public Type generateBytecode(ListExpression result, CodeContext ctx, MethodVisitor mv) {
        EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
        // 创建 Object[] 数组
        mv.visitLdcInsn(result.getElements().size());
        mv.visitTypeInsn(ANEWARRAY, Type.OBJECT.getPath());
        // 遍历所有元素，填充数组
        int index = 0;
        for (ParseResult element : result.getElements()) {
            // 复制数组引用
            mv.visitInsn(Opcodes.DUP);
            // 压入数组索引
            mv.visitLdcInsn(index++);
            // 生成元素的字节码
            Evaluator<ParseResult> eval = registry.getEvaluator(element);
            if (eval == null) {
                throw new RuntimeException("No evaluator found for element");
            }
            boxing(eval.generateBytecode(element, ctx, mv), mv);
            // 存储到数组
            mv.visitInsn(Opcodes.AASTORE);
        }
        // 调用 Arrays.asList
        mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "asList", "([" + Type.OBJECT + ")Ljava/util/List;", false);
        return Type.OBJECT;
    }
}
