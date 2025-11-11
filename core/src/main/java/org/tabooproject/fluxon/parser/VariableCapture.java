package org.tabooproject.fluxon.parser;

public class VariableCapture {

    public final String name;
    public final int sourceIndex;
    public final int lambdaIndex;

    public VariableCapture(String name, int sourceIndex, int lambdaIndex) {
        this.name = name;
        this.sourceIndex = sourceIndex;
        this.lambdaIndex = lambdaIndex;
    }
}
