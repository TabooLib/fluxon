package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.parser.definitions.Definitions;
import java.util.List;

/**
 * 用户自定义的函数
 */
public class UserFunction implements Function {
    
    private final Definitions.FunctionDefinition definition;
    private final Environment closure;
    private final Interpreter interpreter;
    
    public UserFunction(Definitions.FunctionDefinition definition, Environment closure, Interpreter interpreter) {
        this.definition = definition;
        this.closure = closure;
        this.interpreter = interpreter;
    }
    
    @Override
    public Object call(Object[] args) {
        // 创建新的环境，父环境为函数定义时的环境（闭包）
        Environment functionEnv = new Environment(closure);
        
        // 绑定参数
        List<String> parameters = definition.getParameters();
        int minParamCount = Math.min(parameters.size(), args.length);
        
        // 绑定实际传递的参数
        for (int i = 0; i < minParamCount; i++) {
            functionEnv.define(parameters.get(i), args[i]);
        }
        
        // 未传递的参数赋值为null
        for (int i = minParamCount; i < parameters.size(); i++) {
            functionEnv.define(parameters.get(i), null);
        }
        
        try {
            // 执行函数体
            return interpreter.executeWithEnvironment(definition.getBody(), functionEnv);
        } catch (ReturnValue returnValue) {
            // 捕获返回值
            return returnValue.getValue();
        }
    }
    
    /**
     * 获取函数定义
     * 
     * @return 函数定义
     */
    public Definitions.FunctionDefinition getDefinition() {
        return definition;
    }
    
    /**
     * 检查函数是否为异步函数
     * 
     * @return 是否为异步函数
     */
    public boolean isAsync() {
        return definition.isAsync();
    }
} 