package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;

import java.util.*;

/**
 * 默认字节码生成器实现
 */
public class DefaultBytecodeGenerator implements BytecodeGenerator {

    private final EvaluatorRegistry registry = EvaluatorRegistry.getInstance();

    // 存储字段信息
    private final Map<String, FieldInfo> fields = new LinkedHashMap<>();
    // 存储脚本主体语句
    private final List<Statement> statements = new ArrayList<>();
    // 返回值表达式
    private Expression returnExpression;

    @Override
    public void generateExpressionBytecode(Expression expr, MethodVisitor mv) {
        ExpressionEvaluator<Expression> evaluator = registry.getExpression(expr.getExpressionType());
        if (evaluator != null) {
            evaluator.generateBytecode(expr, mv);
        } else {
            throw new RuntimeException("No evaluator found for expression type: " + expr.getExpressionType());
        }
    }

    @Override
    public void generateStatementBytecode(Statement stmt, MethodVisitor mv) {
        // TODO: 实现语句的字节码生成
    }

    @Override
    public void addField(String name, String type, Expression initializer) {
        fields.put(name, new FieldInfo(type, initializer));
    }

    @Override
    public void setReturnExpression(Expression expr) {
        this.returnExpression = expr;
    }

    @Override
    public void addScriptBody(Statement... statements) {
        Collections.addAll(this.statements, statements);
    }

    @Override
    public byte[] generateClassBytecode(String className) {
        return generateClassBytecode(className, "org/tabooproject/fluxon/runtime/RuntimeScriptBase");
    }

    @Override
    public byte[] generateClassBytecode(String className, String superClassName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        // 生成类，继承 RuntimeScriptBase
        cw.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC,
                className,
                null,
                superClassName,
                null);

        // 生成字段
        for (Map.Entry<String, FieldInfo> entry : fields.entrySet()) {
            cw.visitField(Opcodes.ACC_PRIVATE,
                    entry.getKey(),
                    entry.getValue().type,
                    null,
                    null);
        }

        // 生成返回值字段
        if (returnExpression != null) {
            String resultType = getResultDescriptor(returnExpression);
            cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                    "$$result",
                    resultType,
                    null,
                    null);
            // 生成getter方法
            generateResultGetter(cw, className, resultType);
        }

        // 生成构造函数
        generateConstructor(cw, className);

        cw.visitEnd();
        return cw.toByteArray();
    }

    private void generateConstructor(ClassWriter cw, String className) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                "<init>",
                "(Lorg/tabooproject/fluxon/runtime/Environment;)V",
                null,
                null);
        mv.visitCode();

        // 调用父类构造函数
        mv.visitVarInsn(Opcodes.ALOAD, 0);  // this
        mv.visitVarInsn(Opcodes.ALOAD, 1);  // environment参数
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "org/tabooproject/fluxon/runtime/RuntimeScriptBase",
                "<init>",
                "(Lorg/tabooproject/fluxon/runtime/Environment;)V",
                false);

        for (Map.Entry<String, FieldInfo> entry : fields.entrySet()) {
            if (entry.getValue().initializer != null) {
                mv.visitVarInsn(Opcodes.ALOAD, 0);  // this
                generateExpressionBytecode(entry.getValue().initializer, mv);
                mv.visitFieldInsn(Opcodes.PUTFIELD,
                        className,
                        entry.getKey(),
                        entry.getValue().type);
            }
        }

        for (Statement stmt : statements) {
            generateStatementBytecode(stmt, mv);
        }

        // 设置返回值
        if (returnExpression != null) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);  // this
            generateExpressionBytecode(returnExpression, mv);
            mv.visitFieldInsn(Opcodes.PUTFIELD,
                    className,
                    "$$result",
                    getResultDescriptor(returnExpression));
        }

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(3, 2);  // 栈大小3，局部变量2(this和environment)
        mv.visitEnd();
    }

    private void generateResultGetter(ClassWriter cw, String className, String resultType) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                "getResult",
                "()" + resultType,
                null,
                null);
        mv.visitCode();

        mv.visitVarInsn(Opcodes.ALOAD, 0);  // this
        mv.visitFieldInsn(Opcodes.GETFIELD,
                className,
                "$$result",
                resultType);

        // 根据类型选择返回指令
        switch (resultType) {
            case "I":
                mv.visitInsn(Opcodes.IRETURN);
                break;
            case "J":
                mv.visitInsn(Opcodes.LRETURN);
                break;
            case "F":
                mv.visitInsn(Opcodes.FRETURN);
                break;
            case "D":
                mv.visitInsn(Opcodes.DRETURN);
                break;
            default:
                mv.visitInsn(Opcodes.ARETURN);
                break;
        }

        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    private String getResultDescriptor(Expression expr) {
//        ExpressionType type = expr.getExpressionType();
//        switch (type) {
//            case INT_LITERAL:
//                return "I";
//            case LONG_LITERAL:
//                return "J";
//            case FLOAT_LITERAL:
//                return "F";
//            case DOUBLE_LITERAL:
//                return "D";
//            case STRING_LITERAL:
//                return "Ljava/lang/String;";
//            case BOOLEAN_LITERAL:
//                return "Z";
//            case BINARY:
//                // 根据操作数类型推断
//                BinaryExpression binary = (BinaryExpression) expr;
//                if (binary.getLeft() instanceof Expression) {
//                    return getResultDescriptor((Expression) binary.getRight());
//                }
//                break;
//        }
//        return "Ljava/lang/Object;";
        return "D";
    }

    private static class FieldInfo {
        final String type;
        final Expression initializer;

        FieldInfo(String type, Expression initializer) {
            this.type = type;
            this.initializer = initializer;
        }
    }
}