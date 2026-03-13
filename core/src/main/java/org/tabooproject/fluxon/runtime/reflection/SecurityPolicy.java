package org.tabooproject.fluxon.runtime.reflection;

/**
 * 安全策略接口
 * 控制脚本对 Java 类和成员的访问权限
 *
 * @author sky
 */
public interface SecurityPolicy {

    SecurityPolicy ALLOW_ALL = new SecurityPolicy() {
        @Override
        public boolean isClassAllowed(Class<?> clazz) {
            return true;
        }
        @Override
        public boolean isMemberAllowed(Class<?> clazz, String memberName) {
            return true;
        }
    };

    boolean isClassAllowed(Class<?> clazz);

    boolean isMemberAllowed(Class<?> clazz, String memberName);
}
