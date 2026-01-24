package org.tabooproject.fluxon.runtime.function.extension.reflect;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class ExtensionClass {

    @SuppressWarnings({"DuplicatedCode", "unchecked"})
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Class.class)
                .function("name", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(clazz.getName());
                })
                .function("simpleName", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(clazz.getSimpleName());
                })
                .function("canonicalName", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(clazz.getCanonicalName());
                })
                .function("typeName", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(clazz.getTypeName());
                });
        runtime.registerExtension(Class.class, "fs:reflect")
                .function("isInterface", returns(Type.Z).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(clazz.isInterface());
                })
                .function("isArray", returns(Type.Z).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(clazz.isArray());
                })
                .function("isPrimitive", returns(Type.Z).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(clazz.isPrimitive());
                })
                .function("isAnnotation", returns(Type.Z).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(clazz.isAnnotation());
                })
                .function("isEnum", returns(Type.Z).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(clazz.isEnum());
                })
                .function("isAssignableFrom", returns(Type.Z).params(Type.OBJECT), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    Class<?> other = (Class<?>) context.getRef(0);
                    context.setReturnBool(clazz.isAssignableFrom(other));
                })
                .function("isInstance", returns(Type.Z).params(Type.OBJECT), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(clazz.isInstance(context.getRef(0)));
                })
                .function("superclass", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(clazz.getSuperclass());
                })
                .function("interfaces", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Arrays.asList(clazz.getInterfaces()));
                })
                .function("package", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(clazz.getPackage());
                })
                .function("packageName", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(clazz.getPackage().getName());
                })
                .function("classLoader", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(clazz.getClassLoader());
                })
                .function("modifiers", returns(Type.I).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnInt(clazz.getModifiers());
                })
                .function("isPublic", returns(Type.Z).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isPublic(clazz.getModifiers()));
                })
                .function("isPrivate", returns(Type.Z).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isPrivate(clazz.getModifiers()));
                })
                .function("isProtected", returns(Type.Z).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isProtected(clazz.getModifiers()));
                })
                .function("isAbstract", returns(Type.Z).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isAbstract(clazz.getModifiers()));
                })
                .function("isFinal", returns(Type.Z).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isFinal(clazz.getModifiers()));
                })
                .function("isStatic", returns(Type.Z).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isStatic(clazz.getModifiers()));
                })
                .function("componentType", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(clazz.getComponentType());
                })
                .function("cast", returns(Type.OBJECT).params(Type.OBJECT), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(clazz.cast(context.getRef(0)));
                })
                .function("newInstance", returns(Type.OBJECT).noParams(), context -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        Constructor<?> constructor = clazz.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        context.setReturnRef(constructor.newInstance());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create instance: " + e.getMessage(), e);
                    }
                })
                .function("constructors", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Arrays.asList(clazz.getConstructors()));
                })
                .function("declaredConstructors", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Arrays.asList(clazz.getDeclaredConstructors()));
                })
                .function("methods", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Arrays.asList(clazz.getMethods()));
                })
                .function("declaredMethods", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Arrays.asList(clazz.getDeclaredMethods()));
                })
                .function("fields", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Arrays.asList(clazz.getFields()));
                })
                .function("declaredFields", returns(Type.OBJECT).noParams(), context -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Arrays.asList(clazz.getDeclaredFields()));
                })
                .function("method", returns(Type.OBJECT).params(Type.OBJECT), context -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        String methodName = Objects.toString(context.getRef(0), null);
                        List<Method> methods = new ArrayList<>();
                        for (Method method : clazz.getMethods()) {
                            if (method.getName().equals(methodName)) {
                                methods.add(method);
                            }
                        }
                        context.setReturnRef(methods.size() == 1 ? methods.get(0) : methods);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get method: " + e.getMessage(), e);
                    }
                })
                .function("declaredMethod", returns(Type.OBJECT).params(Type.OBJECT), context -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        String methodName = Objects.toString(context.getRef(0), null);
                        List<Method> methods = new ArrayList<>();
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.getName().equals(methodName)) {
                                methods.add(method);
                            }
                        }
                        context.setReturnRef(methods.size() == 1 ? methods.get(0) : methods);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get declared method: " + e.getMessage(), e);
                    }
                })
                .function("field", returns(Type.OBJECT).params(Type.OBJECT), context -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        String fieldName = Objects.toString(context.getRef(0), null);
                        if (fieldName == null) return;
                        context.setReturnRef(clazz.getField(fieldName));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get field: " + e.getMessage(), e);
                    }
                })
                .function("declaredField", returns(Type.OBJECT).params(Type.OBJECT), context -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        String fieldName = Objects.toString(context.getRef(0), null);
                        if (fieldName == null) return;
                        context.setReturnRef(clazz.getDeclaredField(fieldName));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get declared field: " + e.getMessage(), e);
                    }
                })
                .function("constructor", returns(Type.OBJECT).params(Type.OBJECT), context -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        List<Object> paramTypes = (List<Object>) context.getRef(0);
                        if (paramTypes == null) {
                            context.setReturnRef(clazz.getConstructor());
                            return;
                        }
                        Class<?>[] paramClasses = new Class<?>[paramTypes.size()];
                        for (int i = 0; i < paramTypes.size(); i++) {
                            if (paramTypes.get(i) instanceof Class) {
                                paramClasses[i] = (Class<?>) paramTypes.get(i);
                            } else {
                                paramClasses[i] = paramTypes.get(i).getClass();
                            }
                        }
                        context.setReturnRef(clazz.getConstructor(paramClasses));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get constructor: " + e.getMessage(), e);
                    }
                })
                .function("declaredConstructor", returns(Type.OBJECT).params(Type.OBJECT), context -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        List<Object> paramTypes = (List<Object>) context.getRef(0);
                        if (paramTypes == null) {
                            context.setReturnRef(clazz.getDeclaredConstructor());
                            return;
                        }
                        Class<?>[] paramClasses = new Class<?>[paramTypes.size()];
                        for (int i = 0; i < paramTypes.size(); i++) {
                            if (paramTypes.get(i) instanceof Class) {
                                paramClasses[i] = (Class<?>) paramTypes.get(i);
                            } else {
                                paramClasses[i] = paramTypes.get(i).getClass();
                            }
                        }
                        context.setReturnRef(clazz.getDeclaredConstructor(paramClasses));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get declared constructor: " + e.getMessage(), e);
                    }
                });
    }
}
