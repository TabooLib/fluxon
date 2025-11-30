package org.tabooproject.fluxon.web;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Minimal HTTP backend for executing Fluxon scripts with streaming output and cost limits.
 */
public class WebBackendServer {

    private static final Gson GSON = new Gson();

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        InterpreterServiceConfig config = InterpreterServiceConfig.fromSystem();
        HttpServer server = createServer(port, new InterpreterService(config), Executors.newFixedThreadPool(16));
        server.start();
        System.out.println("Fluxon web backend started on port " + server.getAddress().getPort());
    }

    public static HttpServer createServer(int port, InterpreterService service, Executor executor) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/execute", new ExecuteHandler(service));
        server.setExecutor(executor);
        return server;
    }

    private static final class ExecuteHandler implements HttpHandler {

        private final InterpreterService service;

        private ExecuteHandler(InterpreterService service) {
            this.service = service;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }
            RequestPayload payload = parseRequest(exchange.getRequestBody());
            if (payload == null || payload.source == null || payload.source.isEmpty()) {
                sendError(exchange, 400, "Missing 'source' in request body");
                return;
            }
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", "text/plain; charset=utf-8");
            // Chunked response so prints stream immediately.
            exchange.sendResponseHeaders(200, 0);
            OutputStream responseStream = exchange.getResponseBody();
            try {
                service.execute(payload.source, responseStream);
            } finally {
                responseStream.flush();
                responseStream.close();
            }
        }

        private RequestPayload parseRequest(InputStream input) throws IOException {
            byte[] body = readAllBytes(input);
            if (body.length == 0) {
                return null;
            }
            return GSON.fromJson(new String(body, StandardCharsets.UTF_8), RequestPayload.class);
        }

        private byte[] readAllBytes(InputStream input) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private static final class RequestPayload {
        String source;
    }
}
