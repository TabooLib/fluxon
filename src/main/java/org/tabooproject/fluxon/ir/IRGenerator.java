package org.tabooproject.fluxon.ir;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.expression.*;
import org.tabooproject.fluxon.ast.statement.*;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompilationPhase;
import org.tabooproject.fluxon.ir.instruction.*;
import org.tabooproject.fluxon.ir.type.FunctionType;
import org.tabooproject.fluxon.ir.type.IRTypeFactory;
import org.tabooproject.fluxon.ir.value.*;
import org.tabooproject.fluxon.semantic.Symbol;
import org.tabooproject.fluxon.semantic.SymbolTable;
import org.tabooproject.fluxon.semantic.TypeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IR 生成器
 * 将 AST 转换为 IR
 */
public class IRGenerator implements CompilationPhase<IRModule>, AstVisitor<IRValue> {
    private final IRTypeFactory typeFactory = IRTypeFactory.getInstance();
    private final Map<String, IRValue> symbolMap = new HashMap<>();
    private final Map<String, IRBasicBlock> labelMap = new HashMap<>();
    
    private IRModule currentModule;
    private IRFunction currentFunction;
    private IRBasicBlock currentBlock;
    private SymbolTable symbolTable;
    
    /**
     * 执行 IR 生成
     * 
     * @param context 编译上下文
     * @return IR 模块
     */
    @Override
    public IRModule process(CompilationContext context) {
        // 获取 AST
        Program program = (Program) context.getAttribute("ast");
        
        if (program == null) {
            throw new IllegalStateException("AST not found in compilation context");
        }
        
        // 获取符号表
        symbolTable = (SymbolTable) context.getAttribute("symbolTable");
        
        if (symbolTable == null) {
            throw new IllegalStateException("Symbol table not found in compilation context");
        }
        
        // 创建模块
        currentModule = new IRModule(context.getSourceName());
        
        // 生成 IR
        program.accept(this);
        
        // 将 IR 模块添加到编译上下文
        context.setAttribute("irModule", currentModule);
        
        return currentModule;
    }
    
    @Override
    public IRValue visitProgram(Program node) {
        // 处理全局变量和函数
        for (Stmt stmt : node.getStatements()) {
            if (stmt instanceof VarDeclStmt) {
                // 全局变量
                processGlobalVariable((VarDeclStmt) stmt);
            } else if (stmt instanceof FunctionDeclStmt) {
                // 函数
                processFunction((FunctionDeclStmt) stmt);
            } else if (stmt instanceof AsyncFunctionDeclStmt) {
                // 异步函数
                stmt.accept(this);
            }
        }
        
        return null;
    }
    
    /**
     * 处理全局变量
     * 
     * @param node 变量声明语句
     */
    private void processGlobalVariable(VarDeclStmt node) {
        // 获取变量类型
        IRType type = convertType(node.getTypeAnnotation());
        
        // 创建全局变量
        Constant initializer = null;
        if (node.getInitializer() != null) {
            IRValue initValue = node.getInitializer().accept(this);
            if (initValue instanceof Constant) {
                initializer = (Constant) initValue;
            } else {
                throw new IllegalStateException("Global variable initializer must be a constant");
            }
        }
        
        GlobalVariable global = new GlobalVariable(node.getName(), type, initializer, !node.isVal());
        
        // 添加到模块
        currentModule.addGlobal(node.getName(), global);
        
        // 添加到符号映射
        symbolMap.put(node.getName(), global);
    }
    
    /**
     * 处理函数
     * 
     * @param node 函数声明语句
     */
    private void processFunction(FunctionDeclStmt node) {
        // 获取返回类型
        IRType returnType = convertType(node.getReturnType());
        
        // 获取参数类型
        List<IRType> paramTypes = new ArrayList<>();
        for (FunctionDeclStmt.Parameter param : node.getParameters()) {
            paramTypes.add(convertType(param.getType()));
        }
        
        // 创建函数类型
        FunctionType functionType = (FunctionType) typeFactory.getFunctionType(
                returnType,
                paramTypes.toArray(new IRType[0]));
        
        // 创建函数
        currentFunction = new IRFunction(node.getName(), functionType, currentModule);
        
        // 添加到模块
        currentModule.addFunction(currentFunction);
        
        // 添加到符号映射
        symbolMap.put(node.getName(), currentFunction);
        
        // 创建入口基本块
        currentBlock = currentFunction.createBlock("entry");
        
        // 处理参数
        for (int i = 0; i < node.getParameters().size(); i++) {
            FunctionDeclStmt.Parameter param = node.getParameters().get(i);
            IRType paramType = paramTypes.get(i);
            
            // 创建参数
            Parameter parameter = new Parameter(currentFunction, param.getName(), paramType, i);
            
            // 添加到函数
            currentFunction.addParameter(parameter);
            
            // 为参数分配栈空间
            AllocaInst alloca = new AllocaInst(paramType, param.getName() + ".addr");
            currentBlock.addInstruction(alloca);
            
            // 存储参数值
            StoreInst store = new StoreInst(parameter, alloca);
            currentBlock.addInstruction(store);
            
            // 添加到符号映射
            symbolMap.put(param.getName(), alloca);
        }
        
        // 处理函数体
        if (node.getBody() instanceof BlockExpr) {
            // 如果函数体是块表达式，处理块语句
            BlockStmt block = ((BlockExpr) node.getBody()).getBlock();
            for (Stmt stmt : block.getStatements()) {
                stmt.accept(this);
            }
        } else {
            // 否则，处理表达式
            IRValue result = node.getBody().accept(this);
            
            // 添加返回指令
            ReturnInst ret = new ReturnInst(result);
            currentBlock.addInstruction(ret);
        }
        
        // 如果最后一个基本块没有终止指令，添加返回指令
        if (!currentBlock.hasTerminator()) {
            ReturnInst ret;
            if (returnType.isVoidType()) {
                ret = ReturnInst.createVoid();
            } else {
                // 创建默认返回值
                Constant defaultValue = createDefaultValue(returnType);
                ret = new ReturnInst(defaultValue);
            }
            currentBlock.addInstruction(ret);
        }
        
        // 重置当前函数和基本块
        currentFunction = null;
        currentBlock = null;
    }
    
    @Override
    public IRValue visitBlockStmt(BlockStmt node) {
        // 处理块中的语句
        for (Stmt stmt : node.getStatements()) {
            stmt.accept(this);
        }
        
        return null;
    }
    
    @Override
    public IRValue visitExpressionStmt(ExpressionStmt node) {
        // 处理表达式
        return node.getExpression().accept(this);
    }
    
    @Override
    public IRValue visitVarDeclStmt(VarDeclStmt node) {
        // 获取变量类型
        IRType type = convertType(node.getTypeAnnotation());
        
        // 为变量分配栈空间
        AllocaInst alloca = new AllocaInst(type, node.getName() + ".addr");
        currentBlock.addInstruction(alloca);
        
        // 处理初始化表达式
        if (node.getInitializer() != null) {
            IRValue initValue = node.getInitializer().accept(this);
            
            // 存储初始值
            StoreInst store = new StoreInst(initValue, alloca);
            currentBlock.addInstruction(store);
        }
        
        // 添加到符号映射
        symbolMap.put(node.getName(), alloca);
        
        return null;
    }
    
    @Override
    public IRValue visitReturnStmt(ReturnStmt node) {
        // 处理返回值
        ReturnInst ret;
        if (node.getValue() != null) {
            IRValue value = node.getValue().accept(this);
            ret = new ReturnInst(value);
        } else {
            ret = ReturnInst.createVoid();
        }
        
        // 添加返回指令
        currentBlock.addInstruction(ret);
        
        return null;
    }
    
    @Override
    public IRValue visitBinaryExpr(BinaryExpr node) {
        // 处理左右操作数
        IRValue left = node.getLeft().accept(this);
        IRValue right = node.getRight().accept(this);
        
        // 如果操作数是指针，需要加载值
        left = loadIfPointer(left);
        right = loadIfPointer(right);
        
        // 创建二元运算指令
        BinaryInst.Operator operator = convertBinaryOperator(node.getOperator());
        BinaryInst binary = new BinaryInst(operator, left, right, "binop");
        currentBlock.addInstruction(binary);
        
        return binary;
    }
    
    @Override
    public IRValue visitUnaryExpr(UnaryExpr node) {
        // 处理操作数
        IRValue operand = node.getOperand().accept(this);
        
        // 如果操作数是指针，需要加载值
        operand = loadIfPointer(operand);
        
        // 创建一元运算指令
        UnaryInst.Operator operator = convertUnaryOperator(node.getOperator());
        UnaryInst unary = new UnaryInst(operator, operand, "unop");
        currentBlock.addInstruction(unary);
        
        return unary;
    }
    
    @Override
    public IRValue visitLiteralExpr(LiteralExpr node) {
        // 根据字面量类型创建常量
        switch (node.getType()) {
            case INTEGER:
                return ConstantInt.getI32((Integer) node.getValue());
            case FLOAT:
                return ConstantFloat.getDouble((Double) node.getValue());
            case BOOLEAN:
                return ConstantBool.get((Boolean) node.getValue());
            case STRING:
                return new ConstantString((String) node.getValue());
            default:
                throw new IllegalStateException("Unsupported literal type: " + node.getType());
        }
    }
    
    @Override
    public IRValue visitVariableExpr(VariableExpr node) {
        // 获取变量
        String name = node.getName();
        IRValue variable = symbolMap.get(name);
        
        if (variable == null) {
            throw new IllegalStateException("Variable not found: " + name);
        }
        
        // 如果是引用，需要加载值
        if (node.isReference()) {
            return variable;
        } else {
            return loadIfPointer(variable);
        }
    }
    
    @Override
    public IRValue visitCallExpr(CallExpr node) {
        // 获取函数
        IRValue function = symbolMap.get(node.getName());
        
        if (function == null) {
            throw new IllegalStateException("Function not found: " + node.getName());
        }
        
        // 处理参数
        List<IRValue> args = new ArrayList<>();
        for (Expr arg : node.getArguments()) {
            IRValue argValue = arg.accept(this);
            
            // 如果参数是指针，需要加载值
            argValue = loadIfPointer(argValue);
            
            args.add(argValue);
        }
        
        // 创建函数调用指令
        CallInst call = new CallInst(function, args, "call");
        currentBlock.addInstruction(call);
        
        return call;
    }
    
    @Override
    public IRValue visitIfExpr(IfExpr node) {
        // 处理条件
        IRValue condition = node.getCondition().accept(this);
        
        // 如果条件是指针，需要加载值
        condition = loadIfPointer(condition);
        
        // 创建基本块
        IRBasicBlock thenBlock = currentFunction.createBlock("if.then");
        IRBasicBlock elseBlock = node.getElseBranch() != null
                ? currentFunction.createBlock("if.else")
                : null;
        IRBasicBlock mergeBlock = currentFunction.createBlock("if.end");
        
        // 创建条件分支指令
        CondBranchInst branch = new CondBranchInst(
                condition,
                thenBlock,
                elseBlock != null ? elseBlock : mergeBlock);
        currentBlock.addInstruction(branch);
        
        // 处理 then 分支
        currentBlock = thenBlock;
        IRValue thenValue = node.getThenBranch().accept(this);
        
        // 如果 then 分支没有终止指令，添加跳转指令
        if (!currentBlock.hasTerminator()) {
            BranchInst br = new BranchInst(mergeBlock);
            currentBlock.addInstruction(br);
        }
        
        // 处理 else 分支
        IRValue elseValue = null;
        if (elseBlock != null) {
            currentBlock = elseBlock;
            elseValue = node.getElseBranch().accept(this);
            
            // 如果 else 分支没有终止指令，添加跳转指令
            if (!currentBlock.hasTerminator()) {
                BranchInst br = new BranchInst(mergeBlock);
                currentBlock.addInstruction(br);
            }
        }
        
        // 设置当前基本块为合并基本块
        currentBlock = mergeBlock;
        
        // 如果 if 表达式有值，创建 phi 指令
        if (thenValue != null && (elseValue != null || node.getElseBranch() == null)) {
            PhiInst phi = new PhiInst(thenValue.getType(), "if.result");
            phi.addIncoming(thenBlock, thenValue);
            
            if (elseValue != null) {
                phi.addIncoming(elseBlock, elseValue);
            } else {
                // 如果没有 else 分支，使用默认值
                Constant defaultValue = createDefaultValue(thenValue.getType());
                phi.addIncoming(branch.getParent(), defaultValue);
            }
            
            currentBlock.addInstruction(phi);
            return phi;
        }
        
        return null;
    }
    
    @Override
    public IRValue visitAsyncFunctionDeclStmt(AsyncFunctionDeclStmt node) {
        // 获取返回类型
        IRType returnType = convertType(node.getReturnType());
        
        // 获取参数类型
        List<IRType> paramTypes = new ArrayList<>();
        for (AsyncFunctionDeclStmt.Parameter param : node.getParameters()) {
            paramTypes.add(convertType(param.getType()));
        }
        
        // 创建函数类型
        FunctionType functionType = (FunctionType) typeFactory.getFunctionType(
                returnType,
                paramTypes.toArray(new IRType[0]));
        
        // 创建函数
        currentFunction = new IRFunction(node.getName(), functionType, currentModule);
        currentFunction.setAsync(true); // 标记为异步函数
        
        // 添加到模块
        currentModule.addFunction(currentFunction);
        
        // 添加到符号映射
        symbolMap.put(node.getName(), currentFunction);
        
        // 创建入口基本块
        currentBlock = currentFunction.createBlock("entry");
        
        // 处理参数
        for (int i = 0; i < node.getParameters().size(); i++) {
            AsyncFunctionDeclStmt.Parameter param = node.getParameters().get(i);
            IRType paramType = paramTypes.get(i);
            
            // 创建参数
            Parameter parameter = new Parameter(currentFunction, param.getName(), paramType, i);
            
            // 添加到函数
            currentFunction.addParameter(parameter);
            
            // 为参数分配栈空间
            AllocaInst alloca = new AllocaInst(paramType, param.getName() + ".addr");
            currentBlock.addInstruction(alloca);
            
            // 存储参数值
            StoreInst store = new StoreInst(parameter, alloca);
            currentBlock.addInstruction(store);
            
            // 添加到符号映射
            symbolMap.put(param.getName(), alloca);
        }
        
        // 处理函数体
        if (node.getBody() instanceof BlockExpr) {
            // 如果函数体是块表达式，处理块语句
            BlockStmt block = ((BlockExpr) node.getBody()).getBlock();
            for (Stmt stmt : block.getStatements()) {
                stmt.accept(this);
            }
        } else {
            // 否则，处理表达式
            IRValue result = node.getBody().accept(this);
            
            // 添加返回指令
            ReturnInst ret = new ReturnInst(result);
            currentBlock.addInstruction(ret);
        }
        
        // 如果最后一个基本块没有终止指令，添加返回指令
        if (!currentBlock.hasTerminator()) {
            ReturnInst ret;
            if (returnType.isVoidType()) {
                ret = ReturnInst.createVoid();
            } else {
                // 创建默认返回值
                Constant defaultValue = createDefaultValue(returnType);
                ret = new ReturnInst(defaultValue);
            }
            currentBlock.addInstruction(ret);
        }
        
        // 重置当前函数和基本块
        currentFunction = null;
        currentBlock = null;
        
        return null;
    }
    
    @Override
    public IRValue visitAwaitStmt(AwaitStmt node) {
        // 检查是否在异步函数中
        if (currentFunction == null || !currentFunction.isAsync()) {
            throw new IllegalStateException("Await statement can only be used in async functions");
        }
        
        // 处理等待的表达式
        IRValue expression = node.getExpression().accept(this);
        
        // 如果表达式是指针，需要加载值
        expression = loadIfPointer(expression);
        
        // 创建 await 指令
        AwaitInst await = new AwaitInst(expression, expression.getType(), "await");
        currentBlock.addInstruction(await);
        
        return await;
    }
    
    // 其他访问方法的实现...
    
    /**
     * 如果值是指针，加载其指向的值
     * 
     * @param value 值
     * @return 加载后的值
     */
    private IRValue loadIfPointer(IRValue value) {
        if (value.getType().isPointerType()) {
            LoadInst load = new LoadInst(value, "load");
            currentBlock.addInstruction(load);
            return load;
        }
        return value;
    }
    
    /**
     * 转换类型
     * 
     * @param typeName 类型名称
     * @return IR 类型
     */
    private IRType convertType(String typeName) {
        if (typeName == null) {
            return typeFactory.getInt32Type(); // 默认类型
        }
        
        switch (typeName) {
            case "Int":
                return typeFactory.getInt32Type();
            case "Float":
                return typeFactory.getFloatType();
            case "Boolean":
                return typeFactory.getBoolType();
            case "String":
                return typeFactory.getPointerType(typeFactory.getInt8Type());
            case "Void":
                return typeFactory.getVoidType();
            default:
                return typeFactory.getInt32Type(); // 默认类型
        }
    }
    
    /**
     * 转换二元运算符
     * 
     * @param operator AST 二元运算符
     * @return IR 二元运算符
     */
    private BinaryInst.Operator convertBinaryOperator(BinaryExpr.Operator operator) {
        switch (operator) {
            case ADD:
                return BinaryInst.Operator.ADD;
            case SUBTRACT:
                return BinaryInst.Operator.SUB;
            case MULTIPLY:
                return BinaryInst.Operator.MUL;
            case DIVIDE:
                return BinaryInst.Operator.DIV;
            case MODULO:
                return BinaryInst.Operator.REM;
            case EQUAL:
                return BinaryInst.Operator.EQ;
            case NOT_EQUAL:
                return BinaryInst.Operator.NE;
            case LESS:
                return BinaryInst.Operator.LT;
            case LESS_EQUAL:
                return BinaryInst.Operator.LE;
            case GREATER:
                return BinaryInst.Operator.GT;
            case GREATER_EQUAL:
                return BinaryInst.Operator.GE;
            case AND:
                return BinaryInst.Operator.AND;
            case OR:
                return BinaryInst.Operator.OR;
            default:
                throw new IllegalStateException("Unsupported binary operator: " + operator);
        }
    }
    
    /**
     * 转换一元运算符
     * 
     * @param operator AST 一元运算符
     * @return IR 一元运算符
     */
    private UnaryInst.Operator convertUnaryOperator(UnaryExpr.Operator operator) {
        switch (operator) {
            case NEGATE:
                return UnaryInst.Operator.NEG;
            case NOT:
                return UnaryInst.Operator.NOT;
            default:
                throw new IllegalStateException("Unsupported unary operator: " + operator);
        }
    }
    
    /**
     * 创建默认值
     * 
     * @param type 类型
     * @return 默认值
     */
    private Constant createDefaultValue(IRType type) {
        if (type.isIntegerType()) {
            return ConstantInt.getI32(0);
        } else if (type.isFloatType()) {
            return ConstantFloat.getFloat(0.0f);
        } else if (type.isBooleanType()) {
            return ConstantBool.get(false);
        } else if (type.isStringType()) {
            return new ConstantString("");
        } else {
            // 对于其他类型，返回 null
            return null;
        }
    }
    
    // 未实现的访问方法
    
    @Override
    public IRValue visitWhenExpr(WhenExpr node) {
        throw new UnsupportedOperationException("When expression not implemented yet");
    }
    
    @Override
    public IRValue visitListExpr(ListExpr node) {
        // 处理列表元素
        List<IRValue> elements = new ArrayList<>();
        for (Expr element : node.getElements()) {
            IRValue value = element.accept(this);
            
            // 如果元素是指针，需要加载值
            value = loadIfPointer(value);
            
            elements.add(value);
        }
        
        // 创建列表
        // 这里简化处理，实际上需要根据元素类型创建适当的列表
        // 例如，对于不可变列表，可以使用 List.of() 方法
        // 对于可变列表，可以使用 new ArrayList<>() 方法
        
        // 1. 创建列表对象
        // 假设有一个 createList 函数，用于创建列表
        IRValue createListFunc = symbolMap.get("createList");
        if (createListFunc == null) {
            throw new IllegalStateException("createList function not found");
        }
        
        // 2. 调用 createList 函数
        CallInst createList = new CallInst(createListFunc, new ArrayList<>(), "list");
        currentBlock.addInstruction(createList);
        
        // 3. 添加元素
        // 假设有一个 addToList 函数，用于向列表添加元素
        IRValue addToListFunc = symbolMap.get("addToList");
        if (addToListFunc == null) {
            throw new IllegalStateException("addToList function not found");
        }
        
        for (IRValue element : elements) {
            List<IRValue> args = new ArrayList<>();
            args.add(createList);
            args.add(element);
            
            CallInst addToList = new CallInst(addToListFunc, args, "add");
            currentBlock.addInstruction(addToList);
        }
        
        return createList;
    }
    
    @Override
    public IRValue visitMapExpr(MapExpr node) {
        // 处理字典键值对
        List<IRValue> keys = new ArrayList<>();
        List<IRValue> values = new ArrayList<>();
        
        for (MapExpr.Entry entry : node.getEntries()) {
            // 处理键
            IRValue key = entry.getKey().accept(this);
            key = loadIfPointer(key);
            keys.add(key);
            
            // 处理值
            IRValue value = entry.getValue().accept(this);
            value = loadIfPointer(value);
            values.add(value);
        }
        
        // 创建字典
        // 这里简化处理，实际上需要根据键值类型创建适当的字典
        // 例如，对于不可变字典，可以使用 Map.ofEntries() 方法
        // 对于可变字典，可以使用 new HashMap<>() 方法
        
        // 1. 创建字典对象
        // 假设有一个 createMap 函数，用于创建字典
        IRValue createMapFunc = symbolMap.get("createMap");
        if (createMapFunc == null) {
            throw new IllegalStateException("createMap function not found");
        }
        
        // 2. 调用 createMap 函数
        CallInst createMap = new CallInst(createMapFunc, new ArrayList<>(), "map");
        currentBlock.addInstruction(createMap);
        
        // 3. 添加键值对
        // 假设有一个 putInMap 函数，用于向字典添加键值对
        IRValue putInMapFunc = symbolMap.get("putInMap");
        if (putInMapFunc == null) {
            throw new IllegalStateException("putInMap function not found");
        }
        
        for (int i = 0; i < keys.size(); i++) {
            List<IRValue> args = new ArrayList<>();
            args.add(createMap);
            args.add(keys.get(i));
            args.add(values.get(i));
            
            CallInst putInMap = new CallInst(putInMapFunc, args, "put");
            currentBlock.addInstruction(putInMap);
        }
        
        return createMap;
    }
    
    @Override
    public IRValue visitRangeExpr(RangeExpr node) {
        // 处理范围起始和结束
        IRValue start = node.getStart().accept(this);
        IRValue end = node.getEnd().accept(this);
        
        // 如果起始和结束是指针，需要加载值
        start = loadIfPointer(start);
        end = loadIfPointer(end);
        
        // 创建范围
        // 假设有一个 createRange 函数，用于创建范围
        IRValue createRangeFunc = symbolMap.get("createRange");
        if (createRangeFunc == null) {
            throw new IllegalStateException("createRange function not found");
        }
        
        // 调用 createRange 函数
        List<IRValue> args = new ArrayList<>();
        args.add(start);
        args.add(end);
        args.add(ConstantBool.get(node.isInclusive())); // 是否包含结束值
        
        CallInst createRange = new CallInst(createRangeFunc, args, "range");
        currentBlock.addInstruction(createRange);
        
        return createRange;
    }
    
    @Override
    public IRValue visitSafeAccessExpr(SafeAccessExpr node) {
        // 处理对象表达式
        IRValue object = node.getObject().accept(this);
        
        // 如果对象是指针，需要加载值
        object = loadIfPointer(object);
        
        // 创建基本块
        IRBasicBlock nullCheckBlock = currentFunction.createBlock("null.check");
        IRBasicBlock accessBlock = currentFunction.createBlock("safe.access");
        IRBasicBlock mergeBlock = currentFunction.createBlock("safe.end");
        
        // 创建空检查
        // 比较对象是否为 null
        BinaryInst nullCheck = new BinaryInst(
                BinaryInst.Operator.NE,
                object,
                ConstantInt.getI32(0), // null 在 IR 中表示为 0
                "null.check");
        currentBlock.addInstruction(nullCheck);
        
        // 创建条件分支
        CondBranchInst branch = new CondBranchInst(nullCheck, accessBlock, mergeBlock);
        currentBlock.addInstruction(branch);
        
        // 处理访问块
        currentBlock = accessBlock;
        
        // 访问属性
        // 这里简化处理，实际上需要根据对象类型和属性名生成适当的访问代码
        // 例如，对于结构体，需要使用 GEP 指令获取字段
        // 对于类对象，需要生成虚函数调用或字段访问
        // 这里假设对象有一个名为 property 的方法或字段
        CallInst propertyAccess = new CallInst(
                object,
                new ArrayList<>(),
                "property." + node.getProperty());
        currentBlock.addInstruction(propertyAccess);
        
        // 跳转到合并块
        BranchInst accessToMerge = new BranchInst(mergeBlock);
        currentBlock.addInstruction(accessToMerge);
        
        // 处理合并块
        currentBlock = mergeBlock;
        
        // 创建 phi 指令，合并结果
        PhiInst phi = new PhiInst(propertyAccess.getType(), "safe.result");
        phi.addIncoming(accessBlock, propertyAccess);
        phi.addIncoming(branch.getParent(), ConstantInt.getI32(0)); // null 结果
        
        currentBlock.addInstruction(phi);
        
        return phi;
    }
    
    @Override
    public IRValue visitElvisExpr(ElvisExpr node) {
        // 处理条件表达式
        IRValue condition = node.getCondition().accept(this);
        
        // 如果条件是指针，需要加载值
        condition = loadIfPointer(condition);
        
        // 创建基本块
        IRBasicBlock nullCheckBlock = currentFunction.createBlock("elvis.check");
        IRBasicBlock fallbackBlock = currentFunction.createBlock("elvis.fallback");
        IRBasicBlock mergeBlock = currentFunction.createBlock("elvis.end");
        
        // 创建空检查
        // 比较条件是否为 null
        BinaryInst nullCheck = new BinaryInst(
                BinaryInst.Operator.NE,
                condition,
                ConstantInt.getI32(0), // null 在 IR 中表示为 0
                "null.check");
        currentBlock.addInstruction(nullCheck);
        
        // 创建条件分支
        CondBranchInst branch = new CondBranchInst(nullCheck, mergeBlock, fallbackBlock);
        currentBlock.addInstruction(branch);
        
        // 处理备选块
        currentBlock = fallbackBlock;
        IRValue fallback = node.getFallback().accept(this);
        
        // 跳转到合并块
        BranchInst fallbackToMerge = new BranchInst(mergeBlock);
        currentBlock.addInstruction(fallbackToMerge);
        
        // 处理合并块
        currentBlock = mergeBlock;
        
        // 创建 phi 指令，合并结果
        PhiInst phi = new PhiInst(condition.getType(), "elvis.result");
        phi.addIncoming(branch.getParent(), condition);
        phi.addIncoming(fallbackBlock, fallback);
        
        currentBlock.addInstruction(phi);
        
        return phi;
    }
    
    @Override
    public IRValue visitLambdaExpr(LambdaExpr node) {
        throw new UnsupportedOperationException("Lambda expression not implemented yet");
    }
    
    @Override
    public IRValue visitBlockExpr(BlockExpr node) {
        // 处理块语句
        visitBlockStmt(node.getBlock());
        
        // 块表达式的值是最后一个表达式的值
        List<Stmt> statements = node.getBlock().getStatements();
        if (!statements.isEmpty()) {
            Stmt lastStmt = statements.get(statements.size() - 1);
            if (lastStmt instanceof ExpressionStmt) {
                return ((ExpressionStmt) lastStmt).getExpression().accept(this);
            }
        }
        
        return null;
    }
    
    @Override
    public IRValue visitFunctionDeclStmt(FunctionDeclStmt node) {
        // 函数声明已在 visitProgram 中处理
        return null;
    }
    
    @Override
    public IRValue visitTryCatchStmt(TryCatchStmt node) {
        throw new UnsupportedOperationException("Try-catch statement not implemented yet");
    }
}