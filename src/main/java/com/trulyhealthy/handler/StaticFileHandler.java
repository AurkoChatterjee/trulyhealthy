package com.trulyhealthy.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Serves static files from the "webapp" folder placed next to the runnable jar. */
public class StaticFileHandler implements HttpHandler {

    private final Path root;

    public StaticFileHandler(Path root) {
        this.root = root;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        Path file = root.resolve(path.substring(1)).normalize();
        if (!file.startsWith(root) || !Files.exists(file) || Files.isDirectory(file)) {
            file = root.resolve("index.html"); // simple SPA-style fallback
        }

        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", contentType(file.toString()));
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String contentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html; charset=utf-8";
        if (fileName.endsWith(".css")) return "text/css; charset=utf-8";
        if (fileName.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
