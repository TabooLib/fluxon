package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.util.List;
import java.util.Objects;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class ExtensionList {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(List.class)
                // 获取指定索引的元素
                .function("get", returns(Type.OBJECT).params(Type.I), (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int index = context.getInt(0);
                    context.setReturnRef(list.get(index));
                })
                // 设置指定索引的元素
                .function("set", returns(Type.OBJECT).params(Type.I, Type.OBJECT), (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int index = context.getInt(0);
                    context.setReturnRef(list.set(index, context.getRef(1)));
                })
                // 在指定位置添加元素
                .function("insert", returns(Type.OBJECT).params(Type.I, Type.OBJECT), (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int index = context.getInt(0);
                    list.add(index, context.getRef(1));
                    context.setReturnRef(list);
                })
                // 移除指定索引的元素
                .function("removeAt", returns(Type.OBJECT).params(Type.I), (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int index = context.getInt(0);
                    context.setReturnRef(list.remove(index));
                })
                // 获取元素的索引
                .function("indexOf", returns(Type.I).params(Type.OBJECT), (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    context.setReturnInt(list.indexOf(context.getArgBoxed(0)));
                })
                // 获取元素的最后索引
                .function("lastIndexOf", returns(Type.I).params(Type.OBJECT), (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    context.setReturnInt(list.lastIndexOf(context.getArgBoxed(0)));
                })
                // 获取子列表
                .function("subList", returns(Type.OBJECT).params(Type.I, Type.I), (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int fromIndex = context.getInt(0);
                    int toIndex = context.getInt(1);
                    context.setReturnRef(list.subList(fromIndex, toIndex));
                });
    }
}
