package com.trulyhealthy.dao;

import com.trulyhealthy.config.DBConfig;
import com.trulyhealthy.model.MedicalRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecordDao {

    public int insert(MedicalRecord r) throws SQLException {
        String sql = "INSERT INTO medical_records (patient_id, doctor_id, visit_date, diagnosis, treatment, prescription, notes) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.patientId);
            ps.setInt(2, r.doctorId);
            ps.setDate(3, Date.valueOf(r.visitDate));
            ps.setString(4, r.diagnosis);
            ps.setString(5, r.treatment);
            ps.setString(6, r.prescription);
            ps.setString(7, r.notes);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public List<MedicalRecord> findByPatient(int patientId) throws SQLException {
        String sql = baseSelect() + " WHERE r.patient_id = ? ORDER BY r.visit_date DESC";
        List<MedicalRecord> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    private String baseSelect() {
        return "SELECT r.*, pu.full_name AS patient_name, du.full_name AS doctor_name " +
                "FROM medical_records r " +
                "JOIN users pu ON pu.id = r.patient_id " +
                "JOIN users du ON du.id = r.doctor_id";
    }

    private MedicalRecord map(ResultSet rs) throws SQLException {
        MedicalRecord r = new MedicalRecord();
        r.id = rs.getInt("id");
        r.patientId = rs.getInt("patient_id");
        r.patientName = rs.getString("patient_name");
        r.doctorId = rs.getInt("doctor_id");
        r.doctorName = rs.getString("doctor_name");
        r.visitDate = rs.getDate("visit_date").toString();
        r.diagnosis = rs.getString("diagnosis");
        r.treatment = rs.getString("treatment");
        r.prescription = rs.getString("prescription");
        r.notes = rs.getString("notes");
        return r;
    }
}
