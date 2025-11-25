package org.tabooproject.fluxon.parser;

public interface TrampolineContinuation {

    /**
     * 继续执行解析链，将已有部分结果 value 交给后续 trampoline。
     */
    Trampoline<ParseResult> apply(ParseResult value);
}
