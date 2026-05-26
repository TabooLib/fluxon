package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.runtime.error.ArgumentTypeMismatchError;
import org.tabooproject.fluxon.runtime.reflection.util.TypeCompatibility;

import java.util.Map;

/**
 * FunctionContext 的参数和局部变量存储帧
 * 同一组槽位同时服务函数参数、env-free 局部变量和 Lambda 捕获转换。
 *
 * @author sky
 */
final class FunctionArgumentFrame {

    private static final Object[] EMPTY_REFS = new Object[0];
    private static final long[] EMPTY_PRIMITIVES = new long[0];
    private static final byte[] EMPTY_ARG_TYPES = new byte[0];

    private static final byte TYPE_REF = 0;
    private static final byte TYPE_INT = 'I';
    private static final byte TYPE_LONG = 'J';
    private static final byte TYPE_FLOAT = 'F';
    private static final byte TYPE_DOUBLE = 'D';
    private static final byte TYPE_BOOL = 'Z';

    private long[] primitives;
    private Object[] refs;
    private byte[] argTypes;
    private int capacity;
    private int argumentCount;

    FunctionArgumentFrame() {
        this.refs = EMPTY_REFS;
        this.primitives = EMPTY_PRIMITIVES;
        this.argTypes = EMPTY_ARG_TYPES;
        this.capacity = 0;
    }

    FunctionArgumentFrame(int capacity) {
        this.primitives = new long[capacity];
        this.refs = new Object[capacity];
        this.argTypes = new byte[capacity];
        this.capacity = capacity;
    }

    int getArgumentCount() {
        return argumentCount;
    }

    int getInt(int index) {
        return (int) primitives[index];
    }

    long getLong(int index) {
        return primitives[index];
    }

    double getDouble(int index) {
        return Double.longBitsToDouble(primitives[index]);
    }

    float getFloat(int index) {
        return Float.intBitsToFloat((int) primitives[index]);
    }

    boolean getBool(int index) {
        return primitives[index] != 0;
    }

    long getPrimitive(int index) {
        return primitives[index];
    }

    Object getRef(int index) {
        return refs[index];
    }

    CaptureCell getCaptureCell(int index) {
        Object value = refs[index];
        return value instanceof CaptureCell ? (CaptureCell) value : null;
    }

    String getString(int index) {
        return (String) refs[index];
    }

    void setInt(int index, int v) {
        primitives[index] = v;
        argTypes[index] = TYPE_INT;
    }

    void setLong(int index, long v) {
        primitives[index] = v;
        argTypes[index] = TYPE_LONG;
    }

    void setDouble(int index, double v) {
        primitives[index] = Double.doubleToRawLongBits(v);
        argTypes[index] = TYPE_DOUBLE;
    }

    void setFloat(int index, float v) {
        primitives[index] = Float.floatToRawIntBits(v);
        argTypes[index] = TYPE_FLOAT;
    }

    void setBool(int index, boolean v) {
        primitives[index] = v ? 1 : 0;
        argTypes[index] = TYPE_BOOL;
    }

    void setRef(int index, Object v) {
        refs[index] = v;
        argTypes[index] = TYPE_REF;
    }

    boolean isArgPrimitive(int index) {
        return index < argTypes.length && argTypes[index] != TYPE_REF;
    }

    byte getArgType(int index) {
        return index < argTypes.length ? argTypes[index] : TYPE_REF;
    }

    double getAsDouble(int index) {
        byte t = getArgType(index);
        if (t == TYPE_REF) return ((Number) refs[index]).doubleValue();
        switch (t) {
            case TYPE_LONG:
                return (double) primitives[index];
            case TYPE_FLOAT:
                return Float.intBitsToFloat((int) primitives[index]);
            case TYPE_DOUBLE:
                return Double.longBitsToDouble(primitives[index]);
            default:
                return (int) primitives[index];
        }
    }

    int getAsInt(int index) {
        byte t = getArgType(index);
        if (t == TYPE_REF) return ((Number) refs[index]).intValue();
        switch (t) {
            case TYPE_LONG:
                return (int) primitives[index];
            case TYPE_FLOAT:
                return (int) Float.intBitsToFloat((int) primitives[index]);
            case TYPE_DOUBLE:
                return (int) Double.longBitsToDouble(primitives[index]);
            default:
                return (int) primitives[index];
        }
    }

    long getAsLong(int index) {
        byte t = getArgType(index);
        if (t == TYPE_REF) return ((Number) refs[index]).longValue();
        switch (t) {
            case TYPE_INT:
                return (int) primitives[index];
            case TYPE_FLOAT:
                return (long) Float.intBitsToFloat((int) primitives[index]);
            case TYPE_DOUBLE:
                return (long) Double.longBitsToDouble(primitives[index]);
            default:
                return primitives[index];
        }
    }

    float getAsFloat(int index) {
        byte t = getArgType(index);
        if (t == TYPE_REF) return ((Number) refs[index]).floatValue();
        switch (t) {
            case TYPE_LONG:
                return (float) primitives[index];
            case TYPE_DOUBLE:
                return (float) Double.longBitsToDouble(primitives[index]);
            case TYPE_FLOAT:
                return Float.intBitsToFloat((int) primitives[index]);
            default:
                return (int) primitives[index];
        }
    }

    boolean getAsBoolean(int index) {
        byte t = getArgType(index);
        if (t == TYPE_REF) {
            Object ref = refs[index];
            if (ref instanceof Boolean) return (Boolean) ref;
            if (ref instanceof Number) return ((Number) ref).doubleValue() != 0;
            return ref != null;
        }
        switch (t) {
            case TYPE_DOUBLE:
                return Double.longBitsToDouble(primitives[index]) != 0;
            case TYPE_FLOAT:
                return Float.intBitsToFloat((int) primitives[index]) != 0;
            default:
                return primitives[index] != 0;
        }
    }

    Object getArgBoxed(int index) {
        if (index >= argumentCount) return null;
        byte t = getArgType(index);
        if (t == TYPE_REF) return refs[index];
        switch (t) {
            case TYPE_INT:
                return (int) primitives[index];
            case TYPE_LONG:
                return primitives[index];
            case TYPE_FLOAT:
                return Float.intBitsToFloat((int) primitives[index]);
            case TYPE_DOUBLE:
                return Double.longBitsToDouble(primitives[index]);
            case TYPE_BOOL:
                return primitives[index] != 0;
            default:
                return refs[index];
        }
    }

    /**
     * Export 直连路径不能依赖 CHECKCAST 抛错，否则会丢失 Fluxon 的参数错误协议。
     */
    void checkArgumentType(FunctionContext<?> context, int index, Class<?> expect) {
        if (index >= argumentCount) return;
        byte type = getArgType(index);
        if (type == TYPE_REF) {
            Object value = refs[index];
            if (value == null) return;
            if (isContextArgumentCompatible(expect, value.getClass())) {
                // ClassBridge 后续会按目标类型 CHECKCAST，兼容成功时必须同步写回转换后的值。
                refs[index] = TypeCompatibility.convertValue(value, expect);
                return;
            }
            throw new ArgumentTypeMismatchError(context, index, expect, value);
        }
        if (isContextArgumentCompatible(expect, primitiveWrapperClass(type))) return;
        throw new ArgumentTypeMismatchError(context, index, expect, getArgBoxed(index));
    }

    void updateRefs(@NotNull Object[] args) {
        this.refs = args;
        this.argumentCount = args.length;
    }

    void resetRefs(@NotNull Object[] refs) {
        this.refs = refs;
        this.argumentCount = refs.length;
        this.argTypes = EMPTY_ARG_TYPES;
        this.capacity = 0;
    }

    void resetCapacity(int argCount) {
        ensureCapacity(argCount);
        this.argumentCount = argCount;
    }

    Type[] collectArgTypes() {
        Type[] types = new Type[argumentCount];
        for (int i = 0; i < argumentCount; i++) {
            byte t = argTypes[i];
            if (t == TYPE_REF) {
                Object ref = refs[i];
                types[i] = ref != null ? Type.fromClass(ref.getClass()) : Type.OBJECT;
            } else {
                types[i] = primitiveByteToType(t);
            }
        }
        return types;
    }

    void convertArgs(FunctionSignature sig, Type[] actualTypes) {
        Type[] expectedTypes = sig.getParameterTypes();
        for (int i = 0; i < argumentCount && i < expectedTypes.length; i++) {
            convertArgType(i, actualTypes[i], expectedTypes[i]);
        }
    }

    /**
     * env-free 解释器路径会把参数槽复用为局部变量槽，这里只扩容不改变实参数量。
     */
    void ensureLocalCapacity(int count) {
        if (capacity < count || refs.length < count || primitives.length < count || argTypes.length < count) {
            long[] newPrimitives = new long[count];
            Object[] newRefs = new Object[count];
            byte[] newArgTypes = new byte[count];
            int primitiveCopy = Math.min(primitives.length, count);
            int refCopy = Math.min(refs.length, count);
            int typeCopy = Math.min(argTypes.length, count);
            System.arraycopy(primitives, 0, newPrimitives, 0, primitiveCopy);
            System.arraycopy(refs, 0, newRefs, 0, refCopy);
            System.arraycopy(argTypes, 0, newArgTypes, 0, typeCopy);
            primitives = newPrimitives;
            refs = newRefs;
            argTypes = newArgTypes;
            capacity = count;
        }
    }

    /**
     * 解释器局部变量路径统一从 refs 读取，原始类型实参需要先装箱回引用槽。
     */
    void normalizeArgsToRef(int paramCount) {
        for (int i = 0; i < paramCount && i < argTypes.length; i++) {
            byte t = argTypes[i];
            if (t != TYPE_REF) {
                switch (t) {
                    case TYPE_INT: refs[i] = (int) primitives[i]; break;
                    case TYPE_LONG: refs[i] = primitives[i]; break;
                    case TYPE_FLOAT: refs[i] = Float.intBitsToFloat((int) primitives[i]); break;
                    case TYPE_DOUBLE: refs[i] = Double.longBitsToDouble(primitives[i]); break;
                    case TYPE_BOOL: refs[i] = primitives[i] != 0; break;
                }
                argTypes[i] = TYPE_REF;
            }
        }
    }

    /**
     * 捕获型 Lambda 的参数槽位不一定从 0 开始，必须按解析期 slot 重新落位。
     */
    void normalizeArgsToParameterSlots(Map<String, Integer> parameters) {
        if (parameters == null || parameters.isEmpty()) return;
        Object[] values = new Object[parameters.size()];
        int argIndex = 0;
        int maxSlot = -1;
        for (Integer slot : parameters.values()) {
            values[argIndex] = getArgBoxed(argIndex);
            if (slot != null && slot > maxSlot) {
                maxSlot = slot;
            }
            argIndex++;
        }
        if (maxSlot >= 0) {
            ensureLocalCapacity(maxSlot + 1);
        }
        argIndex = 0;
        for (Integer slot : parameters.values()) {
            if (slot != null) {
                refs[slot] = values[argIndex];
                argTypes[slot] = TYPE_REF;
            }
            argIndex++;
        }
    }

    Object getLocal(CaptureFrame captureFrame, boolean hasCapturedLocals, int index) {
        if (!hasCapturedLocals) {
            return refs[index];
        }
        if (captureFrame != null && index < captureFrame.size()) {
            return captureFrame.get(index);
        }
        Object value = refs[index];
        return value instanceof CaptureCell ? ((CaptureCell) value).get() : value;
    }

    void setLocal(CaptureFrame captureFrame, boolean hasCapturedLocals, int index, Object value) {
        if (!hasCapturedLocals) {
            refs[index] = value;
            return;
        }
        if (captureFrame != null && index < captureFrame.size()) {
            captureFrame.set(index, value);
            return;
        }
        Object current = refs[index];
        if (current instanceof CaptureCell) {
            ((CaptureCell) current).set(value);
            return;
        }
        refs[index] = value;
    }

    void ensureCaptureCell(int index) {
        Object value = refs[index];
        if (!(value instanceof CaptureCell)) {
            refs[index] = new CaptureCell(value);
        }
    }

    CaptureFrame createCaptureFrame(CaptureFrame captureFrame, int size) {
        CaptureFrame frame = new CaptureFrame(size);
        for (int i = 0; i < size; i++) {
            CaptureCell captured = captureFrame != null ? captureFrame.getCell(i) : null;
            if (captured != null) {
                frame.setCell(i, captured);
                continue;
            }
            ensureCaptureCell(i);
            frame.setCell(i, (CaptureCell) refs[i]);
        }
        return frame;
    }

    void clearIdleReferences() {
        if (capacity == 0) {
            refs = EMPTY_REFS;
            primitives = EMPTY_PRIMITIVES;
            argTypes = EMPTY_ARG_TYPES;
        } else {
            for (int i = 0; i < argumentCount && i < refs.length; i++) {
                refs[i] = null;
            }
        }
        argumentCount = 0;
    }

    private void ensureCapacity(int count) {
        if (capacity < count) {
            primitives = new long[count];
            refs = new Object[count];
            argTypes = new byte[count];
            capacity = count;
        } else if (capacity > count) {
            for (int i = count; i < capacity; i++) {
                refs[i] = null;
            }
        }
    }

    private void convertArgType(int index, Type actual, Type expected) {
        byte t = argTypes[index];
        if (t == TYPE_REF) {
            convertRefArgType(index, expected);
            return;
        }
        if (expected.isPrimitive() && !actual.equals(expected)) {
            convertPrimitiveArgType(index, expected);
        }
    }

    private void convertRefArgType(int index, Type expected) {
        if (!expected.isPrimitive()) {
            Object ref = refs[index];
            // 延迟重载解析确定 Java 签名后，引用参数需要补上脚本字面量到 enum 的转换。
            refs[index] = TypeCompatibility.convertValue(ref, expected.getSource());
            return;
        }
        Object ref = refs[index];
        if (ref instanceof Boolean) {
            convertBooleanArgType(index, (Boolean) ref, expected);
            return;
        }
        if (ref instanceof Number) {
            convertNumberArgType(index, (Number) ref, expected);
        }
    }

    private void convertPrimitiveArgType(int index, Type expected) {
        convertDoubleArgType(index, getAsDouble(index), expected);
    }

    private void convertBooleanArgType(int index, boolean value, Type expected) {
        switch (expected.getDescriptor().charAt(0)) {
            case 'Z':
                setBool(index, value);
                return;
            case 'I':
                setInt(index, value ? 1 : 0);
                return;
            case 'J':
                setLong(index, value ? 1L : 0L);
                return;
            case 'F':
                setFloat(index, value ? 1F : 0F);
                return;
            case 'D':
                setDouble(index, value ? 1D : 0D);
        }
    }

    private void convertNumberArgType(int index, Number value, Type expected) {
        switch (expected.getDescriptor().charAt(0)) {
            case 'I':
            case 'Z':
                setInt(index, value.intValue());
                return;
            case 'J':
                setLong(index, value.longValue());
                return;
            case 'F':
                setFloat(index, value.floatValue());
                return;
            case 'D':
                setDouble(index, value.doubleValue());
        }
    }

    private void convertDoubleArgType(int index, double value, Type expected) {
        switch (expected.getDescriptor().charAt(0)) {
            case 'I':
            case 'Z':
                setInt(index, (int) value);
                return;
            case 'J':
                setLong(index, (long) value);
                return;
            case 'F':
                setFloat(index, (float) value);
                return;
            case 'D':
                setDouble(index, value);
        }
    }

    private static Type primitiveByteToType(byte t) {
        switch (t) {
            case TYPE_INT: return Type.I;
            case TYPE_LONG: return Type.J;
            case TYPE_FLOAT: return Type.F;
            case TYPE_DOUBLE: return Type.D;
            case TYPE_BOOL: return Type.Z;
            default: return Type.OBJECT;
        }
    }

    private static boolean isContextArgumentCompatible(Class<?> expect, Class<?> actual) {
        if (!expect.isPrimitive()) {
            return TypeCompatibility.isTypeCompatible(expect, actual);
        }
        if (expect == boolean.class) {
            return actual == Boolean.class || Number.class.isAssignableFrom(actual);
        }
        if (expect == char.class) {
            return actual == Character.class || Number.class.isAssignableFrom(actual);
        }
        return Number.class.isAssignableFrom(actual) || actual == Character.class || actual == Boolean.class;
    }

    private static Class<?> primitiveWrapperClass(byte type) {
        switch (type) {
            case TYPE_INT:
                return Integer.class;
            case TYPE_LONG:
                return Long.class;
            case TYPE_FLOAT:
                return Float.class;
            case TYPE_DOUBLE:
                return Double.class;
            case TYPE_BOOL:
                return Boolean.class;
            default:
                return Object.class;
        }
    }
}
