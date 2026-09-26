package com.trulyhealthy.dao;

import com.trulyhealthy.config.DBConfig;
import com.trulyhealthy.model.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PatientDao {

    public void upsertProfile(Patient p) throws SQLException {
        String sql = "INSERT INTO patients (user_id, dob, gender, blood_group, contact, address) " +
                "VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                "dob=VALUES(dob), gender=VALUES(gender), blood_group=VALUES(blood_group), " +
                "contact=VALUES(contact), address=VALUES(address)";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, p.userId);
            ps.setDate(2, p.dob != null ? Date.valueOf(p.dob) : null);
            ps.setString(3, p.gender);
            ps.setString(4, p.bloodGroup);
            ps.setString(5, p.contact);
            ps.setString(6, p.address);
            ps.executeUpdate();
        }
    }

    public Optional<Patient> findById(int userId) throws SQLException {
        String sql = "SELECT p.*, u.full_name FROM patients p JOIN users u ON u.id = p.user_id WHERE p.user_id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Patient> findAll() throws SQLException {
        String sql = "SELECT p.*, u.full_name FROM patients p JOIN users u ON u.id = p.user_id ORDER BY u.full_name";
        List<Patient> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    private Patient map(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.userId = rs.getInt("user_id");
        p.fullName = rs.getString("full_name");
        Date dob = rs.getDate("dob");
        p.dob = dob != null ? dob.toString() : null;
        p.gender = rs.getString("gender");
        p.bloodGroup = rs.getString("blood_group");
        p.contact = rs.getString("contact");
        p.address = rs.getString("address");
        return p;
    }
}
