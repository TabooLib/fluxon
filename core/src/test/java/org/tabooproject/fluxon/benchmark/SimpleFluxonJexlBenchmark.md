# FML 基准测试结果

```
Benchmark                                            Mode  Cnt   Score   Error  Units
SimpleFluxonJexlBenchmark.fluxonBooleanLogic         avgt    5   0.841 ± 0.006  us/op
SimpleFluxonJexlBenchmark.fluxonComplexArithmetic    avgt    5   1.353 ± 0.006  us/op
SimpleFluxonJexlBenchmark.fluxonConditional          avgt    5   1.239 ± 0.009  us/op
SimpleFluxonJexlBenchmark.fluxonMixed                avgt    5   3.040 ± 0.025  us/op
SimpleFluxonJexlBenchmark.fluxonSimpleArithmetic     avgt    5   0.917 ± 0.003  us/op
SimpleFluxonJexlBenchmark.fluxonVariableCalculation  avgt    5   1.007 ± 0.006  us/op
SimpleFluxonJexlBenchmark.jexlBooleanLogic           avgt    5  10.166 ± 0.206  us/op
SimpleFluxonJexlBenchmark.jexlComplexArithmetic      avgt    5  12.487 ± 2.495  us/op
SimpleFluxonJexlBenchmark.jexlConditional            avgt    5  15.575 ± 3.323  us/op
SimpleFluxonJexlBenchmark.jexlMixed                  avgt    5  36.876 ± 4.522  us/op
SimpleFluxonJexlBenchmark.jexlSimpleArithmetic       avgt    5  10.927 ± 0.264  us/op
SimpleFluxonJexlBenchmark.jexlVariableCalculation    avgt    5  11.921 ± 0.682  us/op
```

## 测试指标说明

- **Mode**: `avgt` 表示平均执行时间（Average Time）
- **Cnt**: 每个测试执行的样本数（5次）
- **Score**: 平均执行时间
- **Error**: 误差范围（±值）
- **Units**: `us/op` 表示微秒/操作

## 测试对比表格

在无缓存无预编译模式下进行 6 项性能基准测试，结果如下：

| 测试场景  | Fluxon (μs) | JEXL (μs) | 提升倍数  |
|-------|-------------|-----------|-------|
| 布尔逻辑  | 0.841       | 10.166    | 12.1倍 |
| 复杂算术  | 1.353       | 12.487    | 9.2倍  |
| 条件表达式 | 1.239       | 15.575    | 12.6倍 |
| 混合表达式 | 3.040       | 36.876    | 12.1倍 |
| 简单算术  | 0.917       | 10.927    | 11.9倍 |
| 变量计算  | 1.007       | 11.921    | 11.8倍 |
