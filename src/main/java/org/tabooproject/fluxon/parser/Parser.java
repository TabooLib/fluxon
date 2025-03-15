package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.ast.SourceLocation;
import org.tabooproject.fluxon.ast.expression.*;
import org.tabooproject.fluxon.ast.statement.*;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompilationPhase;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * 语法分析器
 * 将词法单元序列转换为 AST
 */
public class Parser implements CompilationPhase<Program> {
    public final List<Token> tokens;
    public int current = 0;
    public final PrattParser prattParser;
    
    /**
     * 创建语法分析器
     * 
     * @param tokens 词法单元序列
     */
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.prattParser = new PrattParser(this);
    }
    
    /**
     * 执行语法分析
     * 
     * @param context 编译上下文
     * @return 程序节点
     */
    @Override
    public Program process(CompilationContext context) {
        List<Stmt> statements = new ArrayList<>();
        boolean strictMode = context.isStrictMode();
        // 解析顶层声明和语句
        while (!isAtEnd()) {
            try {
                Stmt stmt = declaration();
                if (stmt != null) {
                    statements.add(stmt);
                }
            } catch (ParseError error) {
                synchronize();
            }
        }
        
        // 创建程序节点
        return new Program(statements, strictMode, new SourceLocation(1, 1, tokens.get(tokens.size() - 1).getLine(), tokens.get(tokens.size() - 1).getColumn()));
    }
    
    /**
     * 解析声明
     * 
     * @return 声明语句
     */
    public Stmt declaration() {
        if (match(TokenType.DEF)) {
            return functionDeclaration();
        }
        if (match(TokenType.VAL, TokenType.VAR)) {
            return variableDeclaration();
        }
        return statement();
    }
    
    /**
     * 解析函数声明
     * 
     * @return 函数声明语句
     */
    public Stmt functionDeclaration() {
        // 检查是否为异步函数
        boolean isAsync = false;
        if (check(TokenType.ASYNC)) {
            isAsync = true;
            advance();
            consume(TokenType.DEF, "Expect 'def' after 'async'.");
        }
        
        Token name = consume(TokenType.IDENTIFIER, "Expect function name.");
        
        // 解析参数列表
        consume(TokenType.LEFT_PAREN, "Expect '(' after function name.");
        List<FunctionDeclStmt.Parameter> parameters = new ArrayList<>();
        
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                Token paramName = consume(TokenType.IDENTIFIER, "Expect parameter name.");
                
                // 检查是否有类型注解
                String paramType = null;
                if (match(TokenType.COLON)) {
                    Token type = consume(TokenType.IDENTIFIER, "Expect type name after ':'.");
                    paramType = type.getValue();
                }
                
                parameters.add(new FunctionDeclStmt.Parameter(paramName.getValue(), paramType));
            } while (match(TokenType.COMMA));
        }
        
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        
        // 检查是否有返回类型注解
        String returnType = null;
        if (match(TokenType.COLON)) {
            Token type = consume(TokenType.IDENTIFIER, "Expect return type name after ':'.");
            returnType = type.getValue();
        }
        
        // 解析函数体
        consume(TokenType.ASSIGN, "Expect '=' after function declaration.");
        
        Expr body;
        if (match(TokenType.LEFT_BRACE)) {
            Token braceToken = previous();
            BlockStmt blockStmt = (BlockStmt) blockStatement();
            body = new BlockExpr(blockStmt, new SourceLocation(braceToken.getLine(), braceToken.getColumn(), blockStmt.getLocation().getEndLine(), blockStmt.getLocation().getEndColumn()));
        } else {
            body = expression();
        }
        
        if (isAsync) {
            // 将 FunctionDeclStmt.Parameter 转换为 AsyncFunctionDeclStmt.Parameter
            List<AsyncFunctionDeclStmt.Parameter> asyncParameters = new ArrayList<>();
            for (FunctionDeclStmt.Parameter param : parameters) {
                asyncParameters.add(new AsyncFunctionDeclStmt.Parameter(
                    param.getName(), param.getType()
                ));
            }
            return new AsyncFunctionDeclStmt(name.getValue(), asyncParameters, returnType, body, 
                    new SourceLocation(name.getLine(), name.getColumn(), body.getLocation().getEndLine(), body.getLocation().getEndColumn()));
        } else {
            return new FunctionDeclStmt(name.getValue(), parameters, body, returnType, isAsync, 
                    new SourceLocation(name.getLine(), name.getColumn(), body.getLocation().getEndLine(), body.getLocation().getEndColumn()));
        }
    }
    
    /**
     * 解析变量声明
     * 
     * @return 变量声明语句
     */
    public Stmt variableDeclaration() {
        boolean isVal = previous().getType() == TokenType.VAL;
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

        // 检查是否有类型注解
        String typeAnnotation = null;
        if (match(TokenType.COLON)) {
            Token type = consume(TokenType.IDENTIFIER, "Expect type name after ':'.");
            typeAnnotation = type.getValue();
        }

        // 检查是否有初始化表达式
        Expr initializer = null;
        if (match(TokenType.ASSIGN)) {
            initializer = expression();
        }
        return new VarDeclStmt(name.getValue(), initializer, isVal, typeAnnotation,
                new SourceLocation(previous().getLine(), previous().getColumn(),
                        initializer != null ? initializer.getLocation().getEndLine() : name.getLine(),
                        initializer != null ? initializer.getLocation().getEndColumn() : name.getColumn() + name.getValue().length()));
    }
    
    /**
     * 解析表达式语句
     * 
     * @return 表达式语句
     */
    public Stmt expressionStatement() {
        Expr expr = expression();
        // 可选的分号
        match(TokenType.SEMICOLON);
        return new ExpressionStmt(expr, expr.getLocation());
    }
    
    /**
     * 解析块语句
     * 
     * @return 块语句
     */
    public Stmt blockStatement() {
        List<Stmt> statements = new ArrayList<>();
        Token start = previous();
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            Stmt stmt = declaration();
            if (stmt != null) {
                statements.add(stmt);
            }
        }
        Token end = consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return new BlockStmt(statements,
                new SourceLocation(start.getLine(), start.getColumn(), end.getLine(), end.getColumn()));
    }
    
    /**
     * 解析返回语句
     * 
     * @return 返回语句
     */
    public Stmt returnStatement() {
        Token returnToken = previous();
        
        // 解析返回值（如果有）
        Expr value = null;
        if (!check(TokenType.SEMICOLON) && !check(TokenType.RIGHT_BRACE)) {
            value = expression();
        }
        match(TokenType.SEMICOLON); // 可选的分号
        return new ReturnStmt(value, new SourceLocation(returnToken.getLine(), returnToken.getColumn(), value != null ? value.getLocation().getEndLine() : returnToken.getLine(), value != null ? value.getLocation().getEndColumn() : returnToken.getColumn() + "return".length()));
    }
    
    public Stmt awaitStatement() {
        Token awaitToken = previous();
        Expr expression = this.expression();
        match(TokenType.SEMICOLON); // 可选的分号
        return new AwaitStmt(expression, new SourceLocation(awaitToken.getLine(), awaitToken.getColumn(), expression.getLocation().getEndLine(), expression.getLocation().getEndColumn()));
    }
    
    /**
     * 解析语句
     * 
     * @return 语句
     */
    public Stmt statement() {
        if (match(TokenType.IF)) {
            Token ifToken = previous();
            
            // 解析条件表达式
            Expr condition = expression();
            
            // 解析 then 分支
            consume(TokenType.THEN, "Expect 'then' after if condition.");
            Expr thenBranch;
            if (match(TokenType.LEFT_BRACE)) {
                Token braceToken = previous();
                BlockStmt blockStmt = (BlockStmt) blockStatement();
                thenBranch = new BlockExpr(blockStmt, new SourceLocation(braceToken.getLine(), braceToken.getColumn(), blockStmt.getLocation().getEndLine(), blockStmt.getLocation().getEndColumn()));
            } else {
                thenBranch = expression();
            }
            
            // 解析 else 分支（如果有）
            Expr elseBranch = null;
            if (match(TokenType.ELSE)) {
                if (match(TokenType.LEFT_BRACE)) {
                    Token braceToken = previous();
                    BlockStmt blockStmt = (BlockStmt) blockStatement();
                    elseBranch = new BlockExpr(blockStmt, new SourceLocation(braceToken.getLine(), braceToken.getColumn(), blockStmt.getLocation().getEndLine(), blockStmt.getLocation().getEndColumn()));
                } else {
                    elseBranch = expression();
                }
            }
            return new ExpressionStmt(new IfExpr(condition, thenBranch, elseBranch, new SourceLocation(ifToken.getLine(), ifToken.getColumn(), elseBranch != null ? elseBranch.getLocation().getEndLine() : thenBranch.getLocation().getEndLine(), elseBranch != null ? elseBranch.getLocation().getEndColumn() : thenBranch.getLocation().getEndColumn())), new SourceLocation(ifToken.getLine(), ifToken.getColumn(), elseBranch != null ? elseBranch.getLocation().getEndLine() : thenBranch.getLocation().getEndLine(), elseBranch != null ? elseBranch.getLocation().getEndColumn() : thenBranch.getLocation().getEndColumn()));
        }
        
        if (match(TokenType.WHEN)) {
            Token whenToken = previous();
            
            // 解析主体表达式（如果有）
            Expr subject = null;
            if (!check(TokenType.LEFT_BRACE)) {
                subject = expression();
            }
            
            // 解析分支
            consume(TokenType.LEFT_BRACE, "Expect '{' after when.");
            List<WhenExpr.WhenBranch> branches = new ArrayList<>();
            
            while (!check(TokenType.RIGHT_BRACE) && !check(TokenType.ELSE) && !isAtEnd()) {
                // 解析条件
                Expr condition = expression();
                
                // 解析箭头
                consume(TokenType.ARROW, "Expect '->' after when branch condition.");
                
                // 解析分支体
                Expr body;
                if (match(TokenType.LEFT_BRACE)) {
                    Token braceToken = previous();
                    BlockStmt blockStmt = (BlockStmt) blockStatement();
                    body = new BlockExpr(blockStmt, new SourceLocation(braceToken.getLine(), braceToken.getColumn(), blockStmt.getLocation().getEndLine(), blockStmt.getLocation().getEndColumn()));
                } else {
                    body = expression();
                }
                
                branches.add(new WhenExpr.WhenBranch(condition, body));
            }
            
            // 解析 else 分支（如果有）
            Expr elseBranch = null;
            if (match(TokenType.ELSE)) {
                consume(TokenType.ARROW, "Expect '->' after 'else'.");
                elseBranch = expression();
            }
            
            consume(TokenType.RIGHT_BRACE, "Expect '}' after when branches.");
            return new ExpressionStmt(new WhenExpr(subject, branches, elseBranch, new SourceLocation(whenToken.getLine(), whenToken.getColumn(), elseBranch != null ? elseBranch.getLocation().getEndLine() : branches.get(branches.size() - 1).getBody().getLocation().getEndLine(), elseBranch != null ? elseBranch.getLocation().getEndColumn() : branches.get(branches.size() - 1).getBody().getLocation().getEndColumn())), new SourceLocation(whenToken.getLine(), whenToken.getColumn(), elseBranch != null ? elseBranch.getLocation().getEndLine() : branches.get(branches.size() - 1).getBody().getLocation().getEndLine(), elseBranch != null ? elseBranch.getLocation().getEndColumn() : branches.get(branches.size() - 1).getBody().getLocation().getEndColumn()));
        }
        
        if (match(TokenType.LEFT_BRACE)) {
            return blockStatement();
        }
        
        if (match(TokenType.RETURN)) {
            return returnStatement();
        }
        
        if (match(TokenType.TRY)) {
            Token tryToken = previous();
            
            // 解析 try 块
            consume(TokenType.LEFT_BRACE, "Expect '{' after 'try'.");
            BlockStmt tryBlock = (BlockStmt) blockStatement();
            
            // 解析 catch 块
            consume(TokenType.CATCH, "Expect 'catch' after try block.");
            
            // 解析异常参数（如果有）
            String exceptionParam = null;
            if (match(TokenType.LEFT_PAREN)) {
                Token param = consume(TokenType.IDENTIFIER, "Expect exception parameter name.");
                exceptionParam = param.getValue();
                consume(TokenType.RIGHT_PAREN, "Expect ')' after exception parameter.");
            }
            
            consume(TokenType.LEFT_BRACE, "Expect '{' after 'catch'.");
            BlockStmt catchBlock = (BlockStmt) blockStatement();
            return new TryCatchStmt(tryBlock, catchBlock, exceptionParam, null, new SourceLocation(tryToken.getLine(), tryToken.getColumn(), catchBlock.getLocation().getEndLine(), catchBlock.getLocation().getEndColumn()));
        }
        
        if (match(TokenType.AWAIT)) {
            return awaitStatement();
        }
        return expressionStatement();
    }
    
    /**
     * 解析表达式
     * 
     * @return 表达式
     */
    public Expr expression() {
        return prattParser.parseExpression(0);
    }
    
    /**
     * 检查当前词法单元是否为指定类型之一
     * 
     * @param types 要检查的类型数组
     * @return 是否匹配
     */
    public boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查当前词法单元是否为指定类型
     * 
     * @param type 要检查的类型
     * @return 是否匹配
     */
    public boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().getType() == type;
    }
    
    /**
     * 消费当前词法单元并前进
     * 
     * @return 消费的词法单元
     */
    public Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }
    
    /**
     * 检查是否已到达词法单元序列末尾
     * 
     * @return 是否已到达末尾
     */
    public boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }
    
    /**
     * 获取当前词法单元
     * 
     * @return 当前词法单元
     */
    public Token peek() {
        return tokens.get(current);
    }
    
    /**
     * 获取前一个词法单元
     * 
     * @return 前一个词法单元
     */
    public Token previous() {
        return tokens.get(current - 1);
    }
    
    /**
     * 消费指定类型的词法单元
     * 
     * @param type 期望的类型
     * @param message 错误消息
     * @return 消费的词法单元
     */
    public Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        
        throw error(peek(), message);
    }
    
    /**
     * 创建解析错误
     * 
     * @param token 错误位置的词法单元
     * @param message 错误消息
     * @return 解析错误
     */
    public ParseError error(Token token, String message) {
        // 报告错误
        System.err.println("[line " + token.getLine() + "] Error at '" + token.getValue() + "': " + message);
        return new ParseError(message);
    }
    
    /**
     * 同步到下一个语句
     */
    public void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().getType() == TokenType.SEMICOLON) {
                return;
            }
            switch (peek().getType()) {
                case DEF:
                case FUN:
                case VAL:
                case VAR:
                case IF:
                case WHEN:
                case RETURN:
                case TRY:
                    return;
            }
            advance();
        }
    }
    
    /**
     * 解析错误类
     */
    public static class ParseError extends RuntimeException {
        
        public ParseError(String s) {
            super(s);
        }
    }
}