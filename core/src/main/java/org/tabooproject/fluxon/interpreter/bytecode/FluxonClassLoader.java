package org.tabooproject.fluxon.interpreter.bytecode;

/**
 * 用于加载动态生成的类的类加载器
 *
 * <h3>类卸载说明</h3>
 *
 * <p>通过此 ClassLoader 加载的类在以下情况下会被卸载：</p>
 *
 * <ol>
 *   <li><strong>该类的所有实例都已被垃圾回收</strong></li>
 *   <li><strong>加载该类的 ClassLoader 实例已被垃圾回收</strong></li>
 *   <li><strong>该类的 Class 对象没有任何地方被引用</strong></li>
 * </ol>
 *
 * <p><strong>生命周期说明：</strong></p>
 * <ul>
 *   <li>当 {@code new FluxonClassLoader()} 创建的实例失去所有引用时，理论上可以被 GC 回收</li>
 *   <li>ClassLoader 被回收后，通过它加载的所有类也将符合卸载条件</li>
 *   <li>类卸载只在 GC 触发时进行，且不保证立即执行</li>
 *   <li>JVM 实现和配置会影响类卸载的实际行为</li>
 * </ul>
 *
 * <p><strong>使用建议：</strong></p>
 * <ul>
 *   <li>对于临时脚本执行，使用局部变量持有 ClassLoader 实例</li>
 *   <li>执行完毕后及时清理对 Class 对象和实例的引用</li>
 *   <li>避免将动态类的引用存储在静态变量或长生命周期对象中</li>
 * </ul>
 *
 * <p><strong>示例：</strong></p>
 * <pre>{@code
 * // 推荐的使用方式 - 局部作用域，自动清理
 * {
 *     FluxonClassLoader loader = new FluxonClassLoader();
 *     Class<?> scriptClass = loader.defineClass(name, bytecode);
 *     RuntimeScriptBase instance = (RuntimeScriptBase) scriptClass.newInstance();
 *     Object result = instance.eval(environment);
 *     // 方法结束时，loader、scriptClass、instance 都将失去引用
 * }
 * // 此时类具备了卸载的条件，等待下次 GC
 * }</pre>
 */
public class FluxonClassLoader extends ClassLoader {

    // 无参构造器，使用系统类加载器作为父加载器
    public FluxonClassLoader() {
        super();
    }

    public FluxonClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> defineClass(String name, byte[] bytecode) {
        return defineClass(name, bytecode, 0, bytecode.length);
    }
}