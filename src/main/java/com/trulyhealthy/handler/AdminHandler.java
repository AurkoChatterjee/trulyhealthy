package com.trulyhealthy.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.trulyhealthy.dao.DoctorDao;
import com.trulyhealthy.model.Doctor;
import com.trulyhealthy.model.User;
import com.trulyhealthy.util.PasswordUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Admin-only routes:
 *   GET    /api/admin/users            -> list every account
 *   POST   /api/admin/users            -> create a DOCTOR, ADMIN, or RESEARCH account
 *                                          { username, password, fullName, email, role, ...role-specific fields }
 *   PUT    /api/admin/users/{id}/status -> { active: true|false } activate/deactivate
 *   DELETE /api/admin/users/{id}       -> remove an account
 */
public class AdminHandler extends BaseHandler implements HttpHandler {

    private final DoctorDao doctorDao = new DoctorDao();
    private static final List<String> CREATABLE_ROLES = List.of("DOCTOR", "ADMIN", "RESEARCH");

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handlePreflight(ex)) return;
        try {
            Optional<User> callerOpt = requireRole(ex, "ADMIN");
            if (callerOpt.isEmpty()) return;

            String method = ex.getRequestMethod();
            String[] parts = ex.getRequestURI().getPath().split("/"); // "", api, admin, users, [id], [status]

            if ("GET".equalsIgnoreCase(method) && parts.length == 4) {
                listUsers(ex);
            } else if ("POST".equalsIgnoreCase(method) && parts.length == 4) {
                createUser(ex);
            } else if ("PUT".equalsIgnoreCase(method) && parts.length == 6 && "status".equals(parts[5])) {
                setStatus(ex, Integer.parseInt(parts[4]));
            } else if ("DELETE".equalsIgnoreCase(method) && parts.length == 5) {
                deleteUser(ex, Integer.parseInt(parts[4]));
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Invalid user id");
        } catch (Exception e) {
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    private void listUsers(HttpExchange ex) throws Exception {
        List<User.PublicView> views = userDao.findAll().stream()
                .map(User.PublicView::new)
                .collect(Collectors.toList());
        sendJson(ex, 200, views);
    }

    private void createUser(HttpExchange ex) throws Exception {
        Map<String, Object> body = body(ex);
        String username = String.valueOf(body.get("username"));
        String password = String.valueOf(body.get("password"));
        String fullName = String.valueOf(body.get("fullName"));
        String role = String.valueOf(body.get("role"));
        String email = (String) body.get("email");

        if (!CREATABLE_ROLES.contains(role)) {
            sendError(ex, 400, "role must be one of " + CREATABLE_ROLES + " (patients self-register).");
            return;
        }
        if (userDao.findByUsername(username).isPresent()) {
            sendError(ex, 409, "That username is already taken.");
            return;
        }

        User u = new User();
        u.username = username;
        u.passwordHash = PasswordUtil.hash(password);
        u.role = role;
        u.fullName = fullName;
        u.email = email;
        u.active = true;
        int userId = userDao.insert(u);

        if ("DOCTOR".equals(role)) {
            Doctor d = new Doctor();
            d.userId = userId;
            d.specialization = (String) body.get("specialization");
            d.licenseNo = (String) body.get("licenseNo");
            d.contact = (String) body.get("contact");
            doctorDao.upsertProfile(d);
        }

        u.id = userId;
        sendJson(ex, 201, new User.PublicView(u));
    }

    private void setStatus(HttpExchange ex, int userId) throws Exception {
        Map<String, Object> body = body(ex);
        boolean active = Boolean.TRUE.equals(body.get("active"));
        userDao.setActive(userId, active);
        sendJson(ex, 200, Map.of("id", userId, "active", active));
    }

    private void deleteUser(HttpExchange ex, int userId) throws Exception {
        userDao.delete(userId);
        addCors(ex);
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }
}
