package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.parser.expression.GroupingExpression;
import org.tabooproject.fluxon.parser.expression.IfExpression;
import org.tabooproject.fluxon.parser.expression.LogicalExpression;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.parser.expression.UnaryExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.statement.Block;
import org.tabooproject.fluxon.parser.statement.BreakStatement;
import org.tabooproject.fluxon.parser.statement.ContinueStatement;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.interpreter.evaluator.expr.funccall.DirectFunctionHandler;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator.loadOpcode;
import static org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator.storeOpcode;

/**
 * 循环 root 变量缓存规划器
 * 只处理没有调用、命令、lambda、await 等外部观察点的纯计算循环。
 *
 * @author sky
 */
final class LoopRootCachePlanner {

    static Plan planForBody(ParseResult body, CodeContext ctx) {
        RootCacheAnalyzer scanner = new RootCacheAnalyzer(ctx);
        if (!scanner.scan(body, true)) return null;
        return createPlan(scanner.assignedRootNames, ctx, null);
    }

    static Plan planForConditionAndBody(ParseResult condition, ParseResult body, CodeContext ctx) {
        RootCacheAnalyzer scanner = new RootCacheAnalyzer(ctx);
        if (!scanner.scan(condition, false)) return null;
        Set<String> conditionRootNames = new HashSet<>(scanner.referencedRootNames.keySet());
        if (!scanner.scan(body, true)) return null;
        return createPlan(scanner.assignedRootNames, ctx, conditionRootNames);
    }

    static LocalPlan planLocalForConditionAndBody(ParseResult condition, ParseResult body, CodeContext ctx) {
        RootCacheAnalyzer scanner = new RootCacheAnalyzer(ctx);
        if (!scanner.scan(condition, false)) return null;
        if (!scanner.scan(body, true)) return null;
        return createLocalPlan(scanner.assignedLocalPositions, ctx);
    }

    /**
     * 判断循环局部变量是否可以在循环体内直接读取 JVM 槽位
     * 只要循环体可能改写该变量，就必须保留 Environment 读路径。
     */
    static boolean canUseLocalVariableCache(ParseResult body, int position, CodeContext ctx) {
        RootCacheAnalyzer scanner = new RootCacheAnalyzer(ctx, position);
        return scanner.scan(body, true);
    }

    static void emitLoadCaches(Plan plan, CodeContext ctx, MethodVisitor mv) {
        for (CodeContext.RootVariableCache cache : plan.caches.values()) {
            CodeContext.RootVariableCache parentCache = ctx.getRootVariableCache(cache.name);
            if (parentCache != null) {
                mv.visitVarInsn(loadOpcode(parentCache.type), parentCache.slot);
                mv.visitVarInsn(storeOpcode(cache.type), cache.slot);
                continue;
            }
            Object constantValue = ctx.getRootConstantValue(cache.name);
            if (emitRootConstantCacheLoad(constantValue, cache, mv)) {
                continue;
            }
            Instructions.loadEnvironment(mv, ctx);
            mv.visitLdcInsn(cache.name);
            mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getRootVariable", "(" + Type.STRING + ")" + Type.OBJECT, false);
            Instructions.unbox(mv, cache.type);
            mv.visitVarInsn(storeOpcode(cache.type), cache.slot);
        }
    }

    private static boolean emitRootConstantCacheLoad(Object value, CodeContext.RootVariableCache cache, MethodVisitor mv) {
        if (!(value instanceof Number)) return false;
        // 顶层直接赋值已经写入 root map，循环缓存进场只需复用同一个常量值。
        Number number = (Number) value;
        if (cache.type == Type.I) {
            mv.visitLdcInsn(number.intValue());
        } else if (cache.type == Type.J) {
            mv.visitLdcInsn(number.longValue());
        } else if (cache.type == Type.F) {
            mv.visitLdcInsn(number.floatValue());
        } else if (cache.type == Type.D) {
            mv.visitLdcInsn(number.doubleValue());
        } else {
            return false;
        }
        mv.visitVarInsn(storeOpcode(cache.type), cache.slot);
        return true;
    }

    static void emitWriteBackCaches(Plan plan, CodeContext ctx, MethodVisitor mv) {
        for (CodeContext.RootVariableCache cache : plan.caches.values()) {
            CodeContext.RootVariableCache parentCache = ctx.getRootVariableCache(cache.name);
            if (parentCache != null) {
                mv.visitVarInsn(loadOpcode(cache.type), cache.slot);
                mv.visitVarInsn(storeOpcode(parentCache.type), parentCache.slot);
                continue;
            }
            Instructions.loadEnvironment(mv, ctx);
            mv.visitLdcInsn(cache.name);
            mv.visitVarInsn(loadOpcode(cache.type), cache.slot);
            emitBox(cache.type, mv);
            mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setRootVariable", "(" + Type.STRING + Type.OBJECT + ")V", false);
        }
    }

    static void emitLoadLocalCaches(LocalPlan plan, CodeContext ctx, MethodVisitor mv) {
        for (Map.Entry<Integer, CodeContext.InlineLocalVariable> entry : plan.caches.entrySet()) {
            Instructions.loadEnvironment(mv, ctx);
            mv.visitLdcInsn(entry.getKey());
            CodeContext.InlineLocalVariable cache = entry.getValue();
            ReferenceEvaluator.emitGetLocal(cache.type, mv);
            mv.visitVarInsn(storeOpcode(cache.type), cache.slot);
        }
    }

    static void emitWriteBackLocalCaches(LocalPlan plan, CodeContext ctx, MethodVisitor mv) {
        Label skipWriteBack = new Label();
        mv.visitVarInsn(loadOpcode(Type.I), plan.executedSlot);
        mv.visitJumpInsn(IFEQ, skipWriteBack);
        for (Map.Entry<Integer, CodeContext.InlineLocalVariable> entry : plan.caches.entrySet()) {
            Instructions.loadEnvironment(mv, ctx);
            mv.visitLdcInsn(entry.getKey());
            CodeContext.InlineLocalVariable cache = entry.getValue();
            mv.visitVarInsn(loadOpcode(cache.type), cache.slot);
            ReferenceEvaluator.emitSetLocal(cache.type, mv);
        }
        mv.visitLabel(skipWriteBack);
    }

    private static void emitBox(Type type, MethodVisitor mv) {
        switch (type.getDescriptor()) {
            case "I":
                mv.visitMethodInsn(INVOKESTATIC, Type.INT.getPath(), "valueOf", "(I)" + Type.INT, false);
                break;
            case "J":
                mv.visitMethodInsn(INVOKESTATIC, Type.LONG.getPath(), "valueOf", "(J)" + Type.LONG, false);
                break;
            case "F":
                mv.visitMethodInsn(INVOKESTATIC, Type.FLOAT.getPath(), "valueOf", "(F)" + Type.FLOAT, false);
                break;
            case "D":
                mv.visitMethodInsn(INVOKESTATIC, Type.DOUBLE.getPath(), "valueOf", "(D)" + Type.DOUBLE, false);
                break;
            case "Z":
                mv.visitMethodInsn(INVOKESTATIC, Type.BOOLEAN.getPath(), "valueOf", "(Z)" + Type.BOOLEAN, false);
                break;
        }
    }

    private static Plan createPlan(LinkedHashMap<String, Boolean> assignedRootNames, CodeContext ctx, Set<String> conditionRootNames) {
        if (ctx.getTypeAnalyzer() == null || ctx.isEnvFreeMode()) return null;
        if (assignedRootNames.isEmpty()) return null;
        LinkedHashMap<String, CodeContext.RootVariableCache> caches = new LinkedHashMap<>();
        for (String name : assignedRootNames.keySet()) {
            // root cache 会在条件判断前加载变量，未确认已存在的 body-only 赋值必须回退到原环境路径。
            if (!hasSafeRootCacheEntry(name, ctx, conditionRootNames)) return null;
            Type type = ctx.getRootVariableType(name);
            if (!isCacheableRootType(type)) return null;
            int slot = ctx.allocateLocalVar(type);
            caches.put(name, new CodeContext.RootVariableCache(name, type, slot));
        }
        return new Plan(caches);
    }

    private static boolean hasSafeRootCacheEntry(String name, CodeContext ctx, Set<String> conditionRootNames) {
        if (ctx.getRootVariableCache(name) != null) return true;
        if (ctx.getRootConstantValue(name) != null) return true;
        return conditionRootNames != null && conditionRootNames.contains(name);
    }

    private static LocalPlan createLocalPlan(LinkedHashMap<Integer, Boolean> assignedLocalPositions, CodeContext ctx) {
        if (ctx.getTypeAnalyzer() == null || ctx.isEnvFreeMode()) return null;
        if (assignedLocalPositions.isEmpty()) return null;
        LinkedHashMap<Integer, CodeContext.InlineLocalVariable> caches = new LinkedHashMap<>();
        for (Integer position : assignedLocalPositions.keySet()) {
            Type type = ctx.getVariableType(position);
            if (!isCacheableRootType(type)) return null;
            int slot = ctx.allocateLocalVar(type);
            caches.put(position, new CodeContext.InlineLocalVariable(type, slot));
        }
        int executedSlot = ctx.allocateLocalVar(Type.I);
        return new LocalPlan(caches, executedSlot);
    }

    private static boolean isCacheableRootType(Type type) {
        return type == Type.I || type == Type.J || type == Type.F || type == Type.D;
    }

    private static boolean isRootNumericAssignment(TokenType op) {
        return op == TokenType.ASSIGN
                || op == TokenType.PLUS_ASSIGN
                || op == TokenType.MINUS_ASSIGN
                || op == TokenType.MULTIPLY_ASSIGN
                || op == TokenType.DIVIDE_ASSIGN
                || op == TokenType.MODULO_ASSIGN;
    }

    static final class Plan {
        final LinkedHashMap<String, CodeContext.RootVariableCache> caches;

        Plan(LinkedHashMap<String, CodeContext.RootVariableCache> caches) {
            this.caches = caches;
        }
    }

    static final class LocalPlan {
        final LinkedHashMap<Integer, CodeContext.InlineLocalVariable> caches;
        final int executedSlot;

        LocalPlan(LinkedHashMap<Integer, CodeContext.InlineLocalVariable> caches, int executedSlot) {
            this.caches = caches;
            this.executedSlot = executedSlot;
        }
    }

    private static final class RootCacheAnalyzer {
        private final CodeContext ctx;
        private final int protectedLocalPosition;
        private final LinkedHashMap<String, Boolean> assignedRootNames = new LinkedHashMap<>();
        private final LinkedHashMap<Integer, Boolean> assignedLocalPositions = new LinkedHashMap<>();
        private final LinkedHashMap<String, Boolean> referencedRootNames = new LinkedHashMap<>();

        private RootCacheAnalyzer(CodeContext ctx) {
            this(ctx, -1);
        }

        private RootCacheAnalyzer(CodeContext ctx, int protectedLocalPosition) {
            this.ctx = ctx;
            this.protectedLocalPosition = protectedLocalPosition;
        }

        private boolean scan(ParseResult node, boolean allowRootAssignment) {
            if (node == null) return true;
            if (node instanceof Block) {
                for (ParseResult statement : ((Block) node).getStatements()) {
                    if (!scan(statement, allowRootAssignment)) return false;
                }
                return true;
            }
            if (node instanceof ExpressionStatement) {
                return scan(((ExpressionStatement) node).getExpression(), allowRootAssignment);
            }
            if (node instanceof BreakStatement || node instanceof ContinueStatement) {
                return true;
            }
            if (node instanceof Statement) {
                return false;
            }
            if (!(node instanceof Expression)) {
                return true;
            }
            if (node instanceof AssignExpression) {
                return scanAssign((AssignExpression) node, allowRootAssignment);
            }
            if (node instanceof BinaryExpression) {
                BinaryExpression binary = (BinaryExpression) node;
                return scan(binary.getLeft(), allowRootAssignment) && scan(binary.getRight(), allowRootAssignment);
            }
            if (node instanceof LogicalExpression) {
                LogicalExpression logical = (LogicalExpression) node;
                return scan(logical.getLeft(), allowRootAssignment) && scan(logical.getRight(), allowRootAssignment);
            }
            if (node instanceof UnaryExpression) {
                return scan(((UnaryExpression) node).getRight(), allowRootAssignment);
            }
            if (node instanceof GroupingExpression) {
                return scan(((GroupingExpression) node).getExpression(), allowRootAssignment);
            }
            if (node instanceof IfExpression) {
                IfExpression ifExpression = (IfExpression) node;
                return scan(ifExpression.getCondition(), false)
                        && scan(ifExpression.getThenBranch(), allowRootAssignment)
                        && scan(ifExpression.getElseBranch(), allowRootAssignment);
            }
            if (node instanceof FunctionCallExpression) {
                FunctionCallExpression call = (FunctionCallExpression) node;
                if (!DirectFunctionHandler.canInlinePureExpression(call, ctx)) return false;
                for (ParseResult argument : call.getArguments()) {
                    if (!scan(argument, false)) return false;
                }
                return true;
            }
            if (node instanceof ReferenceExpression) {
                ReferenceExpression reference = (ReferenceExpression) node;
                if (reference.getPosition() < 0) {
                    referencedRootNames.put(reference.getIdentifier().getValue(), Boolean.TRUE);
                }
                return true;
            }
            if (node instanceof Identifier) {
                return true;
            }
            return isSimpleLiteral(node);
        }

        private boolean scanAssign(AssignExpression assign, boolean allowRootAssignment) {
            if (protectedLocalPosition >= 0 && assign.getPosition() == protectedLocalPosition) return false;
            if (assign.getTarget() instanceof Identifier && assign.getPosition() < 0) {
                if (!allowRootAssignment) return false;
                TokenType op = assign.getOperator().getType();
                if (!isRootNumericAssignment(op)) return false;
                assignedRootNames.put(((Identifier) assign.getTarget()).getValue(), Boolean.TRUE);
                return scan(assign.getValue(), true);
            }
            if (assign.getTarget() instanceof Identifier) {
                assignedLocalPositions.put(assign.getPosition(), Boolean.TRUE);
                return scan(assign.getValue(), allowRootAssignment);
            }
            return false;
        }

        private boolean isSimpleLiteral(ParseResult node) {
            String name = node.getClass().getSimpleName();
            return name.endsWith("Literal");
        }
    }
}
