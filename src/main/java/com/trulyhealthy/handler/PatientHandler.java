package com.trulyhealthy.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.trulyhealthy.dao.MedicalRecordDao;
import com.trulyhealthy.dao.PatientDao;
import com.trulyhealthy.model.MedicalRecord;
import com.trulyhealthy.model.Patient;
import com.trulyhealthy.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Routes:
 *   GET  /api/patients                -> list all patients (doctor/admin only)
 *   GET  /api/patients/{id}           -> profile (self, doctor, or admin)
 *   PUT  /api/patients/{id}           -> update own profile (patient self, or admin)
 *   GET  /api/patients/{id}/records   -> medical history (self, doctor, or admin)
 *   POST /api/patients/{id}/records   -> add a record (doctor only) { visitDate, diagnosis, treatment, prescription, notes }
 */
public class PatientHandler extends BaseHandler implements HttpHandler {

    private final PatientDao patientDao = new PatientDao();
    private final MedicalRecordDao recordDao = new MedicalRecordDao();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handlePreflight(ex)) return;
        String method = ex.getRequestMethod();
        try {
            String[] parts = ex.getRequestURI().getPath().split("/"); // "", api, patients, [id], [records]

            if ("GET".equalsIgnoreCase(method) && parts.length == 3) {
                listAll(ex);
            } else if ("GET".equalsIgnoreCase(method) && parts.length == 4) {
                getProfile(ex, Integer.parseInt(parts[3]));
            } else if ("PUT".equalsIgnoreCase(method) && parts.length == 4) {
                updateProfile(ex, Integer.parseInt(parts[3]));
            } else if ("GET".equalsIgnoreCase(method) && parts.length == 5 && "records".equals(parts[4])) {
                getRecords(ex, Integer.parseInt(parts[3]));
            } else if ("POST".equalsIgnoreCase(method) && parts.length == 5 && "records".equals(parts[4])) {
                addRecord(ex, Integer.parseInt(parts[3]));
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Invalid patient id");
        } catch (Exception e) {
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    private void listAll(HttpExchange ex) throws Exception {
        Optional<User> caller = requireRole(ex, "DOCTOR", "ADMIN");
        if (caller.isEmpty()) return;
        sendJson(ex, 200, patientDao.findAll());
    }

    private void getProfile(HttpExchange ex, int patientId) throws Exception {
        Optional<User> callerOpt = requireRole(ex);
        if (callerOpt.isEmpty()) return;
        User caller = callerOpt.get();
        if ("PATIENT".equals(caller.role) && caller.id != patientId) {
            sendError(ex, 403, "You can only view your own profile.");
            return;
        }
        if ("RESEARCH".equals(caller.role)) {
            sendError(ex, 403, "Research accounts cannot access individual patient records.");
            return;
        }
        Optional<Patient> p = patientDao.findById(patientId);
        if (p.isEmpty()) { sendError(ex, 404, "Patient not found"); return; }
        sendJson(ex, 200, p.get());
    }

    private void updateProfile(HttpExchange ex, int patientId) throws Exception {
        Optional<User> callerOpt = requireRole(ex);
        if (callerOpt.isEmpty()) return;
        User caller = callerOpt.get();
        if ("PATIENT".equals(caller.role) && caller.id != patientId) {
            sendError(ex, 403, "You can only edit your own profile.");
            return;
        }
        if (!"PATIENT".equals(caller.role) && !"ADMIN".equals(caller.role)) {
            sendError(ex, 403, "Not permitted.");
            return;
        }
        Map<String, Object> body = body(ex);
        Patient p = new Patient();
        p.userId = patientId;
        p.dob = (String) body.get("dob");
        p.gender = (String) body.get("gender");
        p.bloodGroup = (String) body.get("bloodGroup");
        p.contact = (String) body.get("contact");
        p.address = (String) body.get("address");
        patientDao.upsertProfile(p);
        sendJson(ex, 200, patientDao.findById(patientId).orElseThrow());
    }

    private void getRecords(HttpExchange ex, int patientId) throws Exception {
        Optional<User> callerOpt = requireRole(ex);
        if (callerOpt.isEmpty()) return;
        User caller = callerOpt.get();
        if ("PATIENT".equals(caller.role) && caller.id != patientId) {
            sendError(ex, 403, "You can only view your own records.");
            return;
        }
        if ("RESEARCH".equals(caller.role)) {
            sendError(ex, 403, "Research accounts only have access to anonymized aggregate statistics.");
            return;
        }
        List<MedicalRecord> records = recordDao.findByPatient(patientId);
        sendJson(ex, 200, records);
    }

    private void addRecord(HttpExchange ex, int patientId) throws Exception {
        Optional<User> callerOpt = requireRole(ex, "DOCTOR");
        if (callerOpt.isEmpty()) return;
        User doctor = callerOpt.get();

        Map<String, Object> body = body(ex);
        MedicalRecord r = new MedicalRecord();
        r.patientId = patientId;
        r.doctorId = doctor.id;
        r.visitDate = String.valueOf(body.get("visitDate"));
        r.diagnosis = (String) body.get("diagnosis");
        r.treatment = (String) body.get("treatment");
        r.prescription = (String) body.get("prescription");
        r.notes = (String) body.get("notes");
        int id = recordDao.insert(r);
        sendJson(ex, 201, Map.of("id", id, "message", "Record added"));
    }
}
