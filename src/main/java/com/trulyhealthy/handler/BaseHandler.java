package com.trulyhealthy.handler;

import com.sun.net.httpserver.HttpExchange;
import com.trulyhealthy.dao.UserDao;
import com.trulyhealthy.model.User;
import com.trulyhealthy.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

public abstract class BaseHandler {

    protected final UserDao userDao = new UserDao();

    protected void sendJson(HttpExchange ex, int status, Object payload) throws IOException {
        byte[] bytes = JsonUtil.toJson(payload).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        addCors(ex);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendError(HttpExchange ex, int status, String message) throws IOException {
        sendJson(ex, status, Map.of("error", message));
    }

    protected void addCors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    protected boolean handlePreflight(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            addCors(ex);
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    /** Resolves the caller from the "Authorization: Bearer <token>" header, or empty if none/invalid. */
    protected Optional<User> currentUser(HttpExchange ex) throws SQLException {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return Optional.empty();
        String token = header.substring("Bearer ".length()).trim();
        return userDao.findBySessionToken(token);
    }

    /** Returns the caller, or writes a 401 and returns empty if there isn't one / role doesn't match. */
    protected Optional<User> requireRole(HttpExchange ex, String... allowedRoles) throws IOException, SQLException {
        Optional<User> user = currentUser(ex);
        if (user.isEmpty()) {
            sendError(ex, 401, "Authentication required. Please log in.");
            return Optional.empty();
        }
        if (allowedRoles.length > 0) {
            boolean ok = false;
            for (String r : allowedRoles) if (r.equals(user.get().role)) ok = true;
            if (!ok) {
                sendError(ex, 403, "You do not have permission to perform this action.");
                return Optional.empty();
            }
        }
        if (!user.get().active) {
            sendError(ex, 403, "This account has been deactivated. Contact an administrator.");
            return Optional.empty();
        }
        return user;
    }

    protected Map<String, Object> body(HttpExchange ex) throws IOException {
        return JsonUtil.readBody(ex.getRequestBody());
    }

    protected String pathSegment(HttpExchange ex, int index) {
        String[] parts = ex.getRequestURI().getPath().split("/");
        return index < parts.length ? parts[index] : null;
    }

    protected Map<String, String> queryParams(HttpExchange ex) {
        String q = ex.getRequestURI().getQuery();
        Map<String, String> map = new java.util.HashMap<>();
        if (q == null) return map;
        for (String pair : q.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) map.put(kv[0], java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return map;
    }
}
