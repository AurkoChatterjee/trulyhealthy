package com.trulyhealthy.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.trulyhealthy.dao.PatientDao;
import com.trulyhealthy.model.Patient;
import com.trulyhealthy.model.User;
import com.trulyhealthy.util.PasswordUtil;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Routes:
 *   POST /api/auth/login    { username, password }               -> { token, user }
 *   POST /api/auth/logout   (Authorization header)                -> 204
 *   POST /api/auth/register { username, password, fullName,
 *                              email, dob, gender, bloodGroup,
 *                              contact, address }                 -> patient self-signup
 */
public class AuthHandler extends BaseHandler implements HttpHandler {

    private final PatientDao patientDao = new PatientDao();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handlePreflight(ex)) return;
        String path = ex.getRequestURI().getPath();
        try {
            if (path.endsWith("/login") && "POST".equalsIgnoreCase(ex.getRequestMethod())) {
                login(ex);
            } else if (path.endsWith("/logout") && "POST".equalsIgnoreCase(ex.getRequestMethod())) {
                logout(ex);
            } else if (path.endsWith("/register") && "POST".equalsIgnoreCase(ex.getRequestMethod())) {
                register(ex);
            } else if (path.endsWith("/me") && "GET".equalsIgnoreCase(ex.getRequestMethod())) {
                me(ex);
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    private void login(HttpExchange ex) throws Exception {
        Map<String, Object> body = body(ex);
        String username = String.valueOf(body.get("username"));
        String password = String.valueOf(body.get("password"));

        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isEmpty() || !PasswordUtil.verify(password, userOpt.get().passwordHash)) {
            sendError(ex, 401, "Invalid username or password.");
            return;
        }
        User user = userOpt.get();
        if (!user.active) {
            sendError(ex, 403, "This account has been deactivated. Contact an administrator.");
            return;
        }
        String token = userDao.createSession(user.id);
        sendJson(ex, 200, Map.of("token", token, "user", new User.PublicView(user)));
    }

    private void logout(HttpExchange ex) throws Exception {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            userDao.deleteSession(header.substring("Bearer ".length()).trim());
        }
        addCors(ex);
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }

    private void me(HttpExchange ex) throws Exception {
        Optional<User> user = currentUser(ex);
        if (user.isEmpty()) {
            sendError(ex, 401, "Not logged in.");
            return;
        }
        sendJson(ex, 200, new User.PublicView(user.get()));
    }

    /** Patients can self-register; every other role is created by an admin. */
    private void register(HttpExchange ex) throws Exception {
        Map<String, Object> body = body(ex);
        String username = String.valueOf(body.get("username"));
        String password = String.valueOf(body.get("password"));
        String fullName = String.valueOf(body.get("fullName"));
        String email = (String) body.get("email");

        if (username.isBlank() || password.isBlank() || fullName.isBlank()) {
            sendError(ex, 400, "username, password and fullName are required.");
            return;
        }
        if (userDao.findByUsername(username).isPresent()) {
            sendError(ex, 409, "That username is already taken.");
            return;
        }

        User u = new User();
        u.username = username;
        u.passwordHash = PasswordUtil.hash(password);
        u.role = "PATIENT";
        u.fullName = fullName;
        u.email = email;
        u.active = true;
        int userId = userDao.insert(u);

        Patient p = new Patient();
        p.userId = userId;
        p.dob = (String) body.get("dob");
        p.gender = (String) body.get("gender");
        p.bloodGroup = (String) body.get("bloodGroup");
        p.contact = (String) body.get("contact");
        p.address = (String) body.get("address");
        patientDao.upsertProfile(p);

        String token = userDao.createSession(userId);
        u.id = userId;
        sendJson(ex, 201, Map.of("token", token, "user", new User.PublicView(u)));
    }
}
