# Parser 性能测试

本目录包含两种不同的 Parser 性能测试实现：

1. `ParserPerformanceTest.java`：基于 JUnit 5 的简单性能测试
2. `ParserJmhBenchmark.java`：基于 JMH 框架的专业基准测试

## JUnit 5 性能测试

`ParserPerformanceTest` 使用 JUnit 5 的 `@RepeatedTest` 注解和 `System.nanoTime()` 来测量执行时间。这种方法简单直观，适合快速了解 Parser 的性能特征。

### 运行方法

1. 在 IDE 中直接运行 `ParserPerformanceTest` 类
2. 或者使用 Gradle 命令：
   ```
   ./gradlew test --tests ParserPerformanceTest
   ```

## JMH 基准测试

`ParserJmhBenchmark` 使用 JMH (Java Microbenchmark Harness) 框架进行更专业的性能测试。JMH 是一个专门为 Java 代码微基准测试设计的工具，它能够更准确地测量代码的性能，避免 JIT 编译、垃圾回收等因素的干扰。

### 运行方法

1. 在 IDE 中直接运行 `ParserJmhBenchmark` 类的 main 方法
2. 或者使用 Gradle 命令：
   ```
   ./gradlew jmh
   ```

### 测试内容

与 JUnit 5 性能测试相同，但测量结果更加准确。

### JMH 配置说明

```java
@BenchmarkMode(Mode.AverageTime)        // 测量平均执行时间
@OutputTimeUnit(TimeUnit.MICROSECONDS)  // 输出结果单位为微秒
@State(Scope.Benchmark)                 // 状态作用域为整个基准测试
@Fork(value = 1, warmups = 1)           // 使用 1 个分叉，1 个预热分叉
@Warmup(iterations = 3, time = 1)       // 预热 3 次，每次 1 秒
@Measurement(iterations = 5, time = 1)  // 测量 5 次，每次 1 秒
```

### JMH 首次运行结果

```
Benchmark                                     Mode  Cnt   Score   Error  Units
ParserJmhBenchmark.asyncFunctionDefinition    avgt    5   0.620 ± 0.014  us/op
ParserJmhBenchmark.complexFunctionDefinition  avgt    5   0.928 ± 0.021  us/op
ParserJmhBenchmark.largeCode                  avgt    5  75.985 ± 7.097  us/op
ParserJmhBenchmark.mixedCode                  avgt    5   4.345 ± 0.200  us/op
ParserJmhBenchmark.nestedFunctionCall         avgt    5   0.429 ± 0.033  us/op
ParserJmhBenchmark.noBracketFunctionCall      avgt    5   0.405 ± 0.039  us/op
ParserJmhBenchmark.simpleFunctionDefinition   avgt    5   0.384 ± 0.015  us/op
ParserJmhBenchmark.whenExpression             avgt    5   1.221 ± 0.037  us/op
```