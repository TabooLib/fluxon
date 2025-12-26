package org.tabooproject.fluxon.type;

import org.tabooproject.fluxon.runtime.java.Export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于反射访问测试的Java对象类
 * 
 * @author sky
 */
public class TestObject {
    
    // ========== 公共字段 ==========
    public String publicField = "public-value";
    public int intField = 42;
    public boolean booleanField = true;
    public double doubleField = 3.14;
    public long longField = 9999999999L;
    public Integer boxedIntField = 100;
    public String nullableField = null;
    public List<String> listField = Arrays.asList("a", "b", "c");
    public Map<String, Integer> mapField = new HashMap<String, Integer>() {{
        put("one", 1);
        put("two", 2);
    }};
    public String[] arrayField = {"x", "y", "z"};
    public static String staticField = "static-value";
    public final String finalField = "final-value";
    
    // ========== 私有字段 ==========
    private String privateField = "private-value";
    protected String protectedField = "protected-value";
    
    // ========== 嵌套对象 ==========
    public TestObject nested = null;
    
    // ========== 无参方法 ==========
    public String getName() {
        return "test-object";
    }
    
    public int getNumber() {
        return 100;
    }
    
    // ========== 有参方法 ==========
    public String concat(String a, String b) {
        return a + b;
    }
    
    public int add(int a, int b) {
        return a + b;
    }
    
    // ========== 重载方法 ==========
    public String process(String value) {
        return "string:" + value;
    }
    
    public String process(int value) {
        return "int:" + value;
    }
    
    public String process(String a, String b) {
        return "concat:" + a + b;
    }
    
    // ========== 链式调用支持 ==========
    public TestObject getSelf() {
        return this;
    }
    
    public TestObject createNested() {
        TestObject obj = new TestObject();
        obj.publicField = "nested-value";
        return obj;
    }
    
    // ========== 带@Export注解的方法（用于ClassBridge测试）==========
    
    /**
     * 带Export注解的方法，用于测试ClassBridge优先级
     * 当ClassBridge存在时，应该走bridge而不是反射
     */
    @Export
    public String bridgedMethod() {
        return "bridged-result";
    }
    
    @Export
    public String bridgedMethodWithArg(String arg) {
        return "bridged:" + arg;
    }
    
    @Export
    public int bridgedAdd(int a, int b) {
        return a + b + 1000; // 加1000以区分是否走了bridge
    }
    
    // ========== 边界测试方法 ==========
    
    // 返回基本类型
    public boolean isEnabled() {
        return true;
    }
    
    public double getDouble() {
        return 3.14159;
    }
    
    public long getLong() {
        return Long.MAX_VALUE;
    }
    
    public char getChar() {
        return 'X';
    }
    
    public byte getByte() {
        return (byte) 127;
    }
    
    public short getShort() {
        return (short) 32767;
    }
    
    public float getFloat() {
        return 2.718f;
    }
    
    // 返回 null
    public String getNullValue() {
        return null;
    }
    
    public Object getNull() {
        return null;
    }
    
    // 返回 void
    public void voidMethod() {
        // 无返回值
    }
    
    public void voidMethodWithArg(String arg) {
        // 无返回值但有参数
    }
    
    // 返回集合类型
    public List<String> getList() {
        return Arrays.asList("item1", "item2", "item3");
    }
    
    public Map<String, Integer> getMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 1);
        map.put("key2", 2);
        return map;
    }
    
    public String[] getArray() {
        return new String[]{"arr1", "arr2", "arr3"};
    }
    
    public int[] getPrimitiveArray() {
        return new int[]{1, 2, 3, 4, 5};
    }
    
    // 空集合
    public List<String> getEmptyList() {
        return new ArrayList<>();
    }
    
    public Map<String, Integer> getEmptyMap() {
        return new HashMap<>();
    }
    
    // 可变参数
    public String varargs(String... args) {
        return "varargs:" + args.length;
    }
    
    public int varargsSum(int... numbers) {
        int sum = 0;
        for (int n : numbers) sum += n;
        return sum;
    }
    
    // 参数类型测试
    public String processBoolean(boolean value) {
        return "boolean:" + value;
    }
    
    public String processDouble(double value) {
        return "double:" + value;
    }
    
    public String processLong(long value) {
        return "long:" + value;
    }
    
    public String processObject(Object value) {
        return "object:" + (value == null ? "null" : value.getClass().getSimpleName());
    }
    
    public String processNull(String value) {
        return "null-check:" + (value == null ? "is-null" : "not-null");
    }
    
    // 参数数量边界
    public String noArgs() {
        return "no-args";
    }
    
    public String oneArg(String a) {
        return "one:" + a;
    }
    
    public String twoArgs(String a, String b) {
        return "two:" + a + "," + b;
    }
    
    public String threeArgs(String a, String b, String c) {
        return "three:" + a + "," + b + "," + c;
    }
    
    public String fiveArgs(String a, String b, String c, String d, String e) {
        return "five:" + a + "," + b + "," + c + "," + d + "," + e;
    }
    
    public String tenArgs(String a, String b, String c, String d, String e,
                         String f, String g, String h, String i, String j) {
        return "ten:" + a + b + c + d + e + f + g + h + i + j;
    }
    
    // 混合参数类型
    public String mixedArgs(String s, int i, boolean b, double d) {
        return s + ":" + i + ":" + b + ":" + d;
    }
    
    // 抛出异常的方法
    public void throwException() {
        throw new RuntimeException("test-exception");
    }
    
    public String throwExceptionWithReturn() {
        throw new IllegalArgumentException("test-illegal-arg");
    }
    
    public void throwCheckedException() throws Exception {
        throw new Exception("test-checked-exception");
    }
    
    // 深层嵌套
    public TestObject getLevel1() {
        TestObject level1 = new TestObject();
        level1.publicField = "level1-value";
        level1.nested = new TestObject();
        level1.nested.publicField = "level2-value";
        level1.nested.nested = new TestObject();
        level1.nested.nested.publicField = "level3-value";
        return level1;
    }
    
    // 自引用循环
    public TestObject getCircular() {
        TestObject obj = new TestObject();
        obj.nested = obj; // 自引用
        return obj;
    }
    
    // 更多重载
    public String overload() {
        return "overload:0";
    }
    
    public String overload(int a) {
        return "overload:1:" + a;
    }
    
    public String overload(String s) {
        return "overload:1:" + s;
    }
    
    public String overload(int a, int b) {
        return "overload:2:" + a + "," + b;
    }
    
    public String overload(String a, int b) {
        return "overload:2:" + a + ":" + b;
    }
    
    public String overload(int a, String b) {
        return "overload:2:" + a + ":" + b;
    }
    
    public String overload(int a, int b, int c) {
        return "overload:3:" + a + "," + b + "," + c;
    }
    
    // 数值转换测试
    public String intToDouble(double value) {
        return "double:" + value;
    }
    
    public String doubleValue(Double value) {
        return "Double:" + value;
    }
    
    public String intValue(Integer value) {
        return "Integer:" + value;
    }
    
    // 继承类型测试
    public String processCollection(java.util.Collection<?> c) {
        return "collection:" + c.size();
    }
    
    public String processIterable(Iterable<?> it) {
        int count = 0;
        for (Object o : it) count++;
        return "iterable:" + count;
    }
    
    // 特殊字符处理
    public String echoSpecial(String s) {
        return s;
    }
    
    // 带返回值的方法链
    public TestObject mutate(String newValue) {
        this.publicField = newValue;
        return this;
    }
    
    // 静态方法
    public static String staticMethod() {
        return "static-result";
    }
    
    public static int staticAdd(int a, int b) {
        return a + b;
    }
    
    public static TestObject createInstance() {
        return new TestObject();
    }
}
