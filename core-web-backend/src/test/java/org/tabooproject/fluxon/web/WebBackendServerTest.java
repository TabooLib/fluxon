package org.tabooproject.fluxon.web;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebBackendServerTest {

    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        clientPool.shutdownNow();
    }

    @Test
    void slowScriptDoesNotBlockFastScript() throws Exception {
        server = WebBackendServer.createServer(0, new InterpreterService(new InterpreterServiceConfig(1000L, 1L)), Executors.newCachedThreadPool());
        server.start();
        int port = server.getAddress().getPort();

        Callable<ResponseWithTime> slowCall = () -> postAndMeasure(port, "{\"source\":\"sleep(500); print('slow')\"}");
        Callable<ResponseWithTime> fastCall = () -> postAndMeasure(port, "{\"source\":\"print('fast')\"}");

        long start = System.currentTimeMillis();
        Future<ResponseWithTime> slowFuture = clientPool.submit(slowCall);
        // Stagger fast slightly after slow starts
        Thread.sleep(50);
        Future<ResponseWithTime> fastFuture = clientPool.submit(fastCall);

        ResponseWithTime fast = fastFuture.get(3, TimeUnit.SECONDS);
        ResponseWithTime slow = slowFuture.get(3, TimeUnit.SECONDS);

        long fastElapsed = fast.completedAt - start;
        long slowElapsed = slow.completedAt - start;

        assertTrue(fast.body.contains("fast"));
        assertTrue(slow.body.contains("slow"));
        // Fast should complete well before slow finishes (sleep 500ms).
        assertTrue(fastElapsed < 400, "fast request took too long: " + fastElapsed);
        assertTrue(slowElapsed >= 450, "slow request finished unexpectedly early: " + slowElapsed);
    }

    @Test
    void streamingDeliversFirstChunkBeforeSleepCompletes() throws Exception {
        server = WebBackendServer.createServer(0, new InterpreterService(new InterpreterServiceConfig(1000L, 1L)), Executors.newCachedThreadPool());
        server.start();
        int port = server.getAddress().getPort();

        String payload = "{\"source\":\"print('begin')\\nsleep(300)\\nprint('end')\"}";
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/execute").openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setReadTimeout(3000);

        long start = System.currentTimeMillis();
        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        String firstLine;
        String secondLine;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            firstLine = reader.readLine();
            long firstArrived = System.currentTimeMillis() - start;
            secondLine = reader.readLine();
            long total = System.currentTimeMillis() - start;

            assertTrue(firstLine != null && firstLine.contains("begin"));
            assertTrue(firstArrived < 250, "first chunk arrived too late: " + firstArrived + "ms");
            assertTrue(secondLine != null && secondLine.contains("end"));
            assertTrue(total >= 300, "total time unexpectedly short: " + total + "ms");
        }
    }

    private ResponseWithTime postAndMeasure(int port, String json) throws Exception {
        long start = System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/execute").openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        try (OutputStream os = connection.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        long end = System.currentTimeMillis();
        return new ResponseWithTime(sb.toString(), end);
    }

    private static final class ResponseWithTime {
        final String body;
        final long completedAt;

        ResponseWithTime(String body, long completedAt) {
            this.body = body;
            this.completedAt = completedAt;
        }
    }
}
