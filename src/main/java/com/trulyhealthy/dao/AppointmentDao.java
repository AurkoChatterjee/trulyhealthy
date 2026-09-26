package com.trulyhealthy.dao;

import com.trulyhealthy.config.DBConfig;
import com.trulyhealthy.model.Appointment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppointmentDao {

    /** Accepts "yyyy-MM-ddTHH:mm" (from an HTML datetime-local input) or with seconds already present. */
    private static Timestamp toTimestamp(String isoDateTime) {
        String normalized = isoDateTime.replace("T", " ");
        if (normalized.length() == 16) normalized += ":00"; // no seconds supplied
        return Timestamp.valueOf(normalized);
    }

    public int book(int patientId, int doctorId, String isoDateTime, String reason) throws SQLException {
        String sql = "INSERT INTO appointments (patient_id, doctor_id, appointment_time, status, reason) " +
                "VALUES (?,?,?,'PENDING',?)";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            ps.setTimestamp(3, toTimestamp(isoDateTime));
            ps.setString(4, reason);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public boolean updateStatus(int appointmentId, String status) throws SQLException {
        String sql = "UPDATE appointments SET status = ? WHERE id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, appointmentId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean reschedule(int appointmentId, String newIsoDateTime) throws SQLException {
        String sql = "UPDATE appointments SET appointment_time = ?, status = 'RESCHEDULED' WHERE id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, toTimestamp(newIsoDateTime));
            ps.setInt(2, appointmentId);
            return ps.executeUpdate() > 0;
        }
    }

    public Optional<Appointment> findById(int id) throws SQLException {
        String sql = baseSelect() + " WHERE a.id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Appointment> findByPatient(int patientId) throws SQLException {
        String sql = baseSelect() + " WHERE a.patient_id = ? ORDER BY a.appointment_time DESC";
        return listWithIntParam(sql, patientId);
    }

    public List<Appointment> findByDoctor(int doctorId) throws SQLException {
        String sql = baseSelect() + " WHERE a.doctor_id = ? ORDER BY a.appointment_time DESC";
        return listWithIntParam(sql, doctorId);
    }

    public List<Appointment> findAll() throws SQLException {
        String sql = baseSelect() + " ORDER BY a.appointment_time DESC";
        List<Appointment> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    /** True if the doctor already has a non-cancelled appointment at exactly this time. */
    public boolean hasConflict(int doctorId, String isoDateTime) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND appointment_time = ? " +
                "AND status NOT IN ('CANCELLED')";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setTimestamp(2, toTimestamp(isoDateTime));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private List<Appointment> listWithIntParam(String sql, int param) throws SQLException {
        List<Appointment> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    private String baseSelect() {
        return "SELECT a.*, pu.full_name AS patient_name, du.full_name AS doctor_name " +
                "FROM appointments a " +
                "JOIN users pu ON pu.id = a.patient_id " +
                "JOIN users du ON du.id = a.doctor_id";
    }

    private Appointment map(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.id = rs.getInt("id");
        a.patientId = rs.getInt("patient_id");
        a.patientName = rs.getString("patient_name");
        a.doctorId = rs.getInt("doctor_id");
        a.doctorName = rs.getString("doctor_name");
        a.appointmentTime = rs.getTimestamp("appointment_time").toLocalDateTime().toString();
        a.status = rs.getString("status");
        a.reason = rs.getString("reason");
        return a;
    }
}
