package org.tabooproject.fluxon.runtime.error;

/**
 * 当解释执行超出成本限制时抛出
 */
public class ExecutionCostExceededError extends FluxonRuntimeError {

    private final long costLimit;
    private final long costRemaining;
    private final long costPerStep;

    public ExecutionCostExceededError(long costLimit, long costRemaining, long costPerStep) {
        super("Execution cost exceeded (limit=" + costLimit + ", remaining=" + costRemaining + ", step=" + costPerStep + ")");
        this.costLimit = costLimit;
        this.costRemaining = costRemaining;
        this.costPerStep = costPerStep;
    }

    public long getCostLimit() {
        return costLimit;
    }

    public long getCostRemaining() {
        return costRemaining;
    }

    public long getCostPerStep() {
        return costPerStep;
    }
}
