package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.statement.Block;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.Type;

import java.util.List;

import static org.objectweb.asm.Opcodes.POP;

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
    public Type generateBytecode(Block result, CodeContext ctx, MethodVisitor mv) {
        Type last = Type.VOID;
        List<ParseResult> statements = result.getStatements();
        for (int i = 0, statementsSize = statements.size(); i < statementsSize; i++) {
            ParseResult statement = statements.get(i);
            Evaluator<ParseResult> eval = ctx.getEvaluator(statement);
            if (eval == null) {
                throw new RuntimeException("No evaluator found for expression");
            }
            last = eval.generateBytecode(statement, ctx, mv);
            // 如果不是最后一条语句，并且有返回值，则丢弃它
            if (i < statementsSize - 1 && last != Type.VOID) {
                mv.visitInsn(POP);
            }
        }
        return last;
    }
}
