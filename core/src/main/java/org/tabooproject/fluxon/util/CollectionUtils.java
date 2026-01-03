package org.tabooproject.fluxon.util;

import java.util.ArrayList;
import java.util.List;

public class CollectionUtils {

    public static List<Object> copyList(Iterable<Object> iterable) {
        List<Object> list;
        if (iterable instanceof List) {
            list = new ArrayList<>((List<Object>) iterable);
        } else {
            list = new ArrayList<>();
            for (Object element : iterable) {
                list.add(element);
            }
        }
        return list;
    }
}
