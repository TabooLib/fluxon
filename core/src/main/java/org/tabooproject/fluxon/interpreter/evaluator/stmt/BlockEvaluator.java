package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.statement.Block;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.Type;

public class BlockEvaluator extends StatementEvaluator<Block> {

    @Override
    public StatementType getType() {
        return StatementType.BLOCK;
    }

    @Override
    public Object evaluate(Interpreter interpreter, Block result) {
        interpreter.enterScope();
        try {
            Object last = null;
            for (ParseResult statement : result.getStatements()) {
                last = interpreter.evaluate(statement);
            }
            return last;
        } finally {
            interpreter.exitScope();
        }
    }

    @Override
    public Type generateBytecode(Block result, MethodVisitor mv) {
        Type last = Type.VOID;
        for (ParseResult statement : result.getStatements()) {
            EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
            Evaluator<ParseResult> eval = registry.getEvaluator(statement);
            if (eval == null) {
                throw new RuntimeException("No evaluator found for expression");
            }
            last = eval.generateBytecode(statement, mv);
        }
        return last;
    }
}
