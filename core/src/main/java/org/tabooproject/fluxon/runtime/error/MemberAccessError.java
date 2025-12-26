package org.tabooproject.fluxon.runtime.error;

/**
 * 成员访问错误
 * 当反射访问成员时发生权限错误或其他访问问题时抛出
 */
public class MemberAccessError extends FluxonRuntimeError {

    public MemberAccessError(String message) {
        super(message);
    }

    public MemberAccessError(String message, Throwable cause) {
        super(message, cause);
    }
}
