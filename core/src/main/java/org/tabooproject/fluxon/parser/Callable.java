package org.tabooproject.fluxon.parser;

import java.util.List;

public interface Callable {

    List<Integer> getParameterCounts();

    int getMaxParameterCount();

    boolean supportsParameterCount(int count);
}
