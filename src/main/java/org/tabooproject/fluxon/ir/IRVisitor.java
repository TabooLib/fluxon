package org.tabooproject.fluxon.ir;

import org.tabooproject.fluxon.ir.instruction.*;
import org.tabooproject.fluxon.ir.value.*;

/**
 * IR 访问者接口
 * 用于访问 IR 节点
 * 
 * @param <T> 返回类型
 */
public interface IRVisitor<T> {
    
    // 模块和函数
    T visitModule(IRModule node);
    T visitFunction(IRFunction node);
    T visitBasicBlock(IRBasicBlock node);
    
    // 指令
    T visitAllocaInst(AllocaInst node);
    T visitLoadInst(LoadInst node);
    T visitStoreInst(StoreInst node);
    T visitBinaryInst(BinaryInst node);
    T visitUnaryInst(UnaryInst node);
    T visitCallInst(CallInst node);
    T visitReturnInst(ReturnInst node);
    T visitBranchInst(BranchInst node);
    T visitCondBranchInst(CondBranchInst node);
    T visitPhiInst(PhiInst node);
    T visitCastInst(CastInst node);
    T visitAwaitInst(AwaitInst node);
    
    // 值
    T visitConstantInt(ConstantInt node);
    T visitConstantFloat(ConstantFloat node);
    T visitConstantBool(ConstantBool node);
    T visitConstantString(ConstantString node);
    T visitGlobalVariable(GlobalVariable node);
    T visitParameter(Parameter node);
}