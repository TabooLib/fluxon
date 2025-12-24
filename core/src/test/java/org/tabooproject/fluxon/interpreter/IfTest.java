package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonTestUtil;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class IfTest {

    @Test
    public void testIfBranchBreak() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("for i in 1..10 then { if &i == 6 break else print(&i) }");
    }
}
