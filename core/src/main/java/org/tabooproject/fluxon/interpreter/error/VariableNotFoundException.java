package org.tabooproject.fluxon.interpreter.error;

import java.util.List;

/**
 * 没有找到变量异常
 */
public class VariableNotFoundException extends RuntimeException {

    private final List<String> vars;

    public VariableNotFoundException(String message, List<String> vars) {
        super(message);
        this.vars = vars;
    }

    /**
     * 获取当前可用变量
     */
    public List<String> getVars() {
        return vars;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + ", LocalVars: " + getVars();
    }
}
