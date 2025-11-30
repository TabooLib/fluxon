package org.tabooproject.fluxon.web;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InterpreterServiceTest {

    @Test
    void streamsOutputAndResult() throws Exception {
        InterpreterService service = new InterpreterService(new InterpreterServiceConfig(100L, 1L));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        service.execute("print('hello')", buffer);

        String output = buffer.toString(StandardCharsets.UTF_8.name());
        assertTrue(output.contains("hello"));
        assertTrue(output.contains("RESULT:"));
    }

    @Test
    void costLimitStopsInfiniteLoop() throws Exception {
        InterpreterService service = new InterpreterService(new InterpreterServiceConfig(2L, 1L));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        service.execute("while true {}", buffer);

        String output = buffer.toString(StandardCharsets.UTF_8.name());
        assertTrue(output.contains("ERROR:"));
        assertTrue(output.contains("cost exceeded"));
    }
}
