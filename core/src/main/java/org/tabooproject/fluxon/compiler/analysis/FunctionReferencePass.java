package org.tabooproject.fluxon.compiler.analysis;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.expression.ContextCallExpression;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.parser.statement.Block;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 脚本中函数调用引用：遍历全部解析结果 AST，收集每次 {@code name(...)} / {@code ctx :: name(...)}。
 * 写入 {@link ScriptAnalysisKeys#REFERENCED_FUNCTIONS}（被调名全集）。
 * 对每个调用点写入 {@link ResolvedCallSite}（所在函数名、各实参编译期静态值；未求出则为 {@code null}）到 {@link ScriptAnalysisKeys#RESOLVED_FUNCTION_CALLS}。
 * 在 {@link FunctionDefinition} 体内按语句顺序维护局部常量以解析 {@code &局部} 实参；顶层无 enclosing、无局部表。
 */
public final class FunctionReferencePass implements ScriptAnalysisPass {

    @Override
    public void run(List<ParseResult> results, ScriptAnalysisBuilder builder) {
        if (results == null) {
            return;
        }
        List<ResolvedCallSite> sites = new ArrayList<>();
        Set<String> referenced = new LinkedHashSet<>();
        for (ParseResult result : results) {
            if (result instanceof FunctionDefinition) {
                FunctionDefinition def = (FunctionDefinition) result;
                Map<String, Object> locals = new LinkedHashMap<>();
                walk(result, def.getName(), locals, sites, referenced);
            } else {
                walk(result, "", null, sites, referenced);
            }
        }
        builder.put(ScriptAnalysisKeys.REFERENCED_FUNCTIONS, referenced);
        builder.put(ScriptAnalysisKeys.RESOLVED_FUNCTION_CALLS, sites);
        builder.put(ScriptAnalysisKeys.RESOLVED_FUNCTION_CALL_NAMES, new LinkedHashSet<>(referenced));
    }

    private void walk(
            ParseResult node,
            String enclosing,
            Map<String, Object> locals,
            List<ResolvedCallSite> sites,
            Set<String> referenced
    ) {
        if (node == null) {
            return;
        }
        if (node instanceof Block) {
            for (ParseResult stmt : ((Block) node).getStatements()) {
                walk(stmt, enclosing, locals, sites, referenced);
            }
            return;
        }
        if (node instanceof ExpressionStatement) {
            ParseResult expression = ((ExpressionStatement) node).getExpression();
            if (locals != null && expression instanceof AssignExpression) {
                recordLocal((AssignExpression) expression, locals);
                return;
            }
            walk(expression, enclosing, locals, sites, referenced);
            return;
        }
        if (locals != null && node instanceof AssignExpression) {
            recordLocal((AssignExpression) node, locals);
            return;
        }
        if (node instanceof ContextCallExpression) {
            ContextCallExpression ctxCall = (ContextCallExpression) node;
            ParseResult target = ctxCall.getTarget();
            if (target instanceof FunctionCallExpression) {
                collectCall((FunctionCallExpression) target, enclosing, locals, sites, referenced);
                for (ParseResult arg : ((FunctionCallExpression) target).getArguments()) {
                    walk(arg, enclosing, locals, sites, referenced);
                }
            } else {
                walk(target, enclosing, locals, sites, referenced);
            }
            walk(ctxCall.getContext(), enclosing, locals, sites, referenced);
            return;
        }
        if (node instanceof FunctionCallExpression) {
            FunctionCallExpression call = (FunctionCallExpression) node;
            collectCall(call, enclosing, locals, sites, referenced);
            for (ParseResult arg : call.getArguments()) {
                walk(arg, enclosing, locals, sites, referenced);
            }
            return;
        }
        node.forEachChild(child -> walk(child, enclosing, locals, sites, referenced));
    }

    private void recordLocal(AssignExpression assign, Map<String, Object> locals) {
        if (assign.getOperator().getType() != TokenType.ASSIGN) {
            return;
        }
        String name = assign.getName();
        if (name == null || name.isEmpty()) {
            return;
        }
        ParseResult rhs = assign.getValue();
        if (!StaticValueUtil.isStaticallyKnown(rhs, locals)) {
            return;
        }
        locals.put(name, StaticValueUtil.resolve(rhs, locals));
    }

    private void collectCall(
            FunctionCallExpression call,
            String enclosing,
            Map<String, Object> locals,
            List<ResolvedCallSite> sites,
            Set<String> referenced
    ) {
        String functionName = call.getFunctionName();
        if (functionName == null || functionName.isEmpty()) {
            return;
        }
        referenced.add(functionName);
        ParseResult[] args = call.getArguments();
        Object[] resolvedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            ParseResult arg = args[i];
            if (locals != null && StaticValueUtil.isStaticallyKnown(arg, locals)) {
                resolvedArgs[i] = StaticValueUtil.resolve(arg, locals);
            } else if (locals == null && StaticValueUtil.isStaticallyKnown(arg, null)) {
                resolvedArgs[i] = StaticValueUtil.resolve(arg, null);
            } else {
                resolvedArgs[i] = null;
            }
        }
        String enclosingName = enclosing == null || enclosing.isEmpty() ? null : enclosing;
        sites.add(new ResolvedCallSite(functionName, enclosingName, resolvedArgs));
    }
}