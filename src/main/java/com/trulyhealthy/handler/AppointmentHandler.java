package com.trulyhealthy.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.trulyhealthy.dao.AppointmentDao;
import com.trulyhealthy.model.Appointment;
import com.trulyhealthy.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Routes (all require a Bearer token):
 *   GET    /api/appointments                 -> caller's own appointments (patient/doctor), or all (admin)
 *   POST   /api/appointments                 -> patient books { doctorId, appointmentTime, reason }
 *   PUT    /api/appointments/{id}/cancel     -> patient, doctor, or admin
 *   PUT    /api/appointments/{id}/reschedule -> { appointmentTime } — patient or doctor
 *   PUT    /api/appointments/{id}/confirm    -> doctor confirms a pending request
 *   PUT    /api/appointments/{id}/complete   -> doctor marks a visit done
 */
public class AppointmentHandler extends BaseHandler implements HttpHandler {

    private final AppointmentDao appointmentDao = new AppointmentDao();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handlePreflight(ex)) return;
        String method = ex.getRequestMethod();
        try {
            String[] parts = ex.getRequestURI().getPath().split("/"); // "", api, appointments, [id], [action]

            if ("GET".equalsIgnoreCase(method) && parts.length == 3) {
                list(ex);
            } else if ("POST".equalsIgnoreCase(method) && parts.length == 3) {
                book(ex);
            } else if ("PUT".equalsIgnoreCase(method) && parts.length == 5) {
                int id = Integer.parseInt(parts[3]);
                String action = parts[4];
                switch (action) {
                    case "cancel" -> cancel(ex, id);
                    case "reschedule" -> reschedule(ex, id);
                    case "confirm" -> setStatus(ex, id, "CONFIRMED", "DOCTOR", "ADMIN");
                    case "complete" -> setStatus(ex, id, "COMPLETED", "DOCTOR", "ADMIN");
                    default -> sendError(ex, 404, "Unknown action: " + action);
                }
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Invalid appointment id");
        } catch (Exception e) {
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    private void list(HttpExchange ex) throws Exception {
        Optional<User> callerOpt = requireRole(ex);
        if (callerOpt.isEmpty()) return;
        User caller = callerOpt.get();

        List<Appointment> result = switch (caller.role) {
            case "PATIENT" -> appointmentDao.findByPatient(caller.id);
            case "DOCTOR" -> appointmentDao.findByDoctor(caller.id);
            case "ADMIN" -> appointmentDao.findAll();
            default -> List.of(); // RESEARCH has no per-patient appointment access
        };
        sendJson(ex, 200, result);
    }

    private void book(HttpExchange ex) throws Exception {
        Optional<User> callerOpt = requireRole(ex, "PATIENT");
        if (callerOpt.isEmpty()) return;
        User caller = callerOpt.get();

        Map<String, Object> body = body(ex);
        int doctorId;
        try {
            doctorId = Integer.parseInt(String.valueOf(body.get("doctorId")));
        } catch (NumberFormatException nfe) {
            sendError(ex, 400, "A valid doctor must be selected.");
            return;
        }
        String appointmentTime = String.valueOf(body.get("appointmentTime"));
        String reason = (String) body.getOrDefault("reason", "");

        if (appointmentDao.hasConflict(doctorId, appointmentTime)) {
            sendError(ex, 409, "That doctor already has an appointment at this time. Please pick another slot.");
            return;
        }
        int id = appointmentDao.book(caller.id, doctorId, appointmentTime, reason);
        Appointment created = appointmentDao.findById(id).orElseThrow();
        sendJson(ex, 201, created);
    }

    private void cancel(HttpExchange ex, int id) throws Exception {
        Optional<User> callerOpt = requireRole(ex);
        if (callerOpt.isEmpty()) return;
        User caller = callerOpt.get();

        Optional<Appointment> appt = appointmentDao.findById(id);
        if (appt.isEmpty()) { sendError(ex, 404, "Appointment not found"); return; }
        if (notAuthorizedFor(caller, appt.get())) { sendError(ex, 403, "Not your appointment."); return; }

        appointmentDao.updateStatus(id, "CANCELLED");
        sendJson(ex, 200, appointmentDao.findById(id).orElseThrow());
    }

    private void reschedule(HttpExchange ex, int id) throws Exception {
        Optional<User> callerOpt = requireRole(ex);
        if (callerOpt.isEmpty()) return;
        User caller = callerOpt.get();

        Optional<Appointment> apptOpt = appointmentDao.findById(id);
        if (apptOpt.isEmpty()) { sendError(ex, 404, "Appointment not found"); return; }
        Appointment appt = apptOpt.get();
        if (notAuthorizedFor(caller, appt)) { sendError(ex, 403, "Not your appointment."); return; }

        Map<String, Object> body = body(ex);
        String newTime = String.valueOf(body.get("appointmentTime"));
        if (appointmentDao.hasConflict(appt.doctorId, newTime)) {
            sendError(ex, 409, "The doctor is already booked at that new time.");
            return;
        }
        appointmentDao.reschedule(id, newTime);
        sendJson(ex, 200, appointmentDao.findById(id).orElseThrow());
    }

    private void setStatus(HttpExchange ex, int id, String status, String... allowedRoles) throws Exception {
        Optional<User> callerOpt = requireRole(ex, allowedRoles);
        if (callerOpt.isEmpty()) return;
        User caller = callerOpt.get();

        Optional<Appointment> apptOpt = appointmentDao.findById(id);
        if (apptOpt.isEmpty()) { sendError(ex, 404, "Appointment not found"); return; }
        if ("DOCTOR".equals(caller.role) && apptOpt.get().doctorId != caller.id) {
            sendError(ex, 403, "Not your appointment.");
            return;
        }
        appointmentDao.updateStatus(id, status);
        sendJson(ex, 200, appointmentDao.findById(id).orElseThrow());
    }

    private boolean notAuthorizedFor(User caller, Appointment appt) {
        if ("ADMIN".equals(caller.role)) return false;
        if ("PATIENT".equals(caller.role)) return appt.patientId != caller.id;
        if ("DOCTOR".equals(caller.role)) return appt.doctorId != caller.id;
        return true;
    }
}
