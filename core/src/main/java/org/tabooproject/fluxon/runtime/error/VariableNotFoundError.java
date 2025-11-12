package org.tabooproject.fluxon.runtime.error;

import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntimeError;

import java.util.List;

/**
 * 变量未找到错误（运行时）
 */
public class VariableNotFoundError extends FluxonRuntimeError {

    private final Environment environment;
    private final String variableName;
    private final int index;
    private final List<String> availableVariables;

    public VariableNotFoundError(Environment environment, String variableName, int index, List<String> availableVariables) {
        super(variableName + ", index: " + index + ", available: " + availableVariables);
        this.environment = environment;
        this.variableName = variableName;
        this.index = index;
        this.availableVariables = availableVariables;
    }

    public VariableNotFoundError(Environment environment, String variableName, List<String> availableVariables) {
        this(environment, variableName, -1, availableVariables);
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getVariableName() {
        return variableName;
    }

    public int getIndex() {
        return index;
    }

    public List<String> getAvailableVariables() {
        return availableVariables;
    }
}
