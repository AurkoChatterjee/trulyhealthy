package com.trulyhealthy.dao;

import com.trulyhealthy.config.DBConfig;
import com.trulyhealthy.model.Doctor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DoctorDao {

    public void upsertProfile(Doctor d) throws SQLException {
        String sql = "INSERT INTO doctors (user_id, specialization, license_no, contact) " +
                "VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE " +
                "specialization=VALUES(specialization), license_no=VALUES(license_no), contact=VALUES(contact)";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, d.userId);
            ps.setString(2, d.specialization);
            ps.setString(3, d.licenseNo);
            ps.setString(4, d.contact);
            ps.executeUpdate();
        }
    }

    public List<Doctor> findAll() throws SQLException {
        String sql = "SELECT d.*, u.full_name FROM doctors d JOIN users u ON u.id = d.user_id " +
                "WHERE u.active = TRUE ORDER BY u.full_name";
        List<Doctor> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public Optional<Doctor> findById(int userId) throws SQLException {
        String sql = "SELECT d.*, u.full_name FROM doctors d JOIN users u ON u.id = d.user_id WHERE d.user_id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private Doctor map(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.userId = rs.getInt("user_id");
        d.fullName = rs.getString("full_name");
        d.specialization = rs.getString("specialization");
        d.licenseNo = rs.getString("license_no");
        d.contact = rs.getString("contact");
        return d;
    }
}
