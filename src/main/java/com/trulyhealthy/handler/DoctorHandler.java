package com.trulyhealthy.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.trulyhealthy.dao.DoctorDao;
import com.trulyhealthy.model.Doctor;
import com.trulyhealthy.model.User;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Routes:
 *   GET /api/doctors        -> any authenticated user (patients need this to book)
 *   GET /api/doctors/{id}   -> a single doctor's public profile
 *   PUT /api/doctors/{id}   -> doctor updates own profile, or admin
 */
public class DoctorHandler extends BaseHandler implements HttpHandler {

    private final DoctorDao doctorDao = new DoctorDao();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handlePreflight(ex)) return;
        String method = ex.getRequestMethod();
        try {
            String[] parts = ex.getRequestURI().getPath().split("/"); // "", api, doctors, [id]

            if ("GET".equalsIgnoreCase(method) && parts.length == 3) {
                listAll(ex);
            } else if ("GET".equalsIgnoreCase(method) && parts.length == 4) {
                getOne(ex, Integer.parseInt(parts[3]));
            } else if ("PUT".equalsIgnoreCase(method) && parts.length == 4) {
                update(ex, Integer.parseInt(parts[3]));
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Invalid doctor id");
        } catch (Exception e) {
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    private void listAll(HttpExchange ex) throws Exception {
        Optional<User> caller = requireRole(ex);
        if (caller.isEmpty()) return;
        sendJson(ex, 200, doctorDao.findAll());
    }

    private void getOne(HttpExchange ex, int id) throws Exception {
        Optional<User> caller = requireRole(ex);
        if (caller.isEmpty()) return;
        Optional<Doctor> d = doctorDao.findById(id);
        if (d.isEmpty()) { sendError(ex, 404, "Doctor not found"); return; }
        sendJson(ex, 200, d.get());
    }

    private void update(HttpExchange ex, int id) throws Exception {
        Optional<User> callerOpt = requireRole(ex, "DOCTOR", "ADMIN");
        if (callerOpt.isEmpty()) return;
        User caller = callerOpt.get();
        if ("DOCTOR".equals(caller.role) && caller.id != id) {
            sendError(ex, 403, "You can only edit your own profile.");
            return;
        }
        Map<String, Object> body = body(ex);
        Doctor d = new Doctor();
        d.userId = id;
        d.specialization = (String) body.get("specialization");
        d.licenseNo = (String) body.get("licenseNo");
        d.contact = (String) body.get("contact");
        doctorDao.upsertProfile(d);
        sendJson(ex, 200, doctorDao.findById(id).orElseThrow());
    }
}
