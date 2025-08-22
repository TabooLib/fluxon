package org.tabooproject.fluxon.compiler;

import java.util.ArrayList;
import java.util.List;

public class FluxonFeatures {

    /**
     * 全局特性：是否允许无括号调用（Kether 风格）
     * 当启用时，允许函数调用省略括号，例如：print "hello" 等同于 print("hello")
     * 默认值：false（禁用），保持标准的函数调用语法
     */
    public static boolean DEFAULT_ALLOW_KETHER_STYLE_CALL = false;
    
    /**
     * 全局特性：是否允许无效引用
     * 当启用时，对未定义的变量或函数引用不会抛出错误，而是返回 null 或默认值
     * 默认值：false（禁用），严格检查所有引用的有效性
     */
    public static boolean DEFAULT_ALLOW_INVALID_REFERENCE = false;

    /**
     * 全局特性：是否允许使用 import 语句
     * 当启用时，允许在脚本中导入其他包，并使用其成员
     * 默认值：true（启用），允许使用 import 语句
     */
    public static boolean DEFAULT_ALLOW_IMPORT = true;

    /**
     * 全局特性：自动导入的包
     * 将自动导入这些包，无需在脚本中显式导入
     * 默认值：无
     */
    public static List<String> DEFAULT_PACKET_AUTO_IMPORT = new ArrayList<>();

    /**
     * 全局特性：禁用的包
     * 将禁止在脚本中导入这些包
     * 默认值：无
     */
    public static List<String> DEFAULT_PACKAGE_BLACKLIST = new ArrayList<>();
}
