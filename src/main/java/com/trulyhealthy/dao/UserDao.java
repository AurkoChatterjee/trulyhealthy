package com.trulyhealthy.dao;

import com.trulyhealthy.config.DBConfig;
import com.trulyhealthy.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserDao {

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<User> findByRole(String role) throws SQLException {
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY full_name";
        List<User> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY role, full_name";
        List<User> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public int insert(User u) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role, full_name, email, active) VALUES (?,?,?,?,?,?)";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.username);
            ps.setString(2, u.passwordHash);
            ps.setString(3, u.role);
            ps.setString(4, u.fullName);
            ps.setString(5, u.email);
            ps.setBoolean(6, u.active);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void setActive(int userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET active = ? WHERE id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void delete(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    // ── sessions ─────────────────────────────────────────────────
    public String createSession(int userId) throws SQLException {
        String token = UUID.randomUUID().toString().replace("-", "");
        String sql = "INSERT INTO sessions (token, user_id) VALUES (?, ?)";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
        return token;
    }

    public Optional<User> findBySessionToken(String token) throws SQLException {
        String sql = "SELECT u.* FROM users u JOIN sessions s ON u.id = s.user_id WHERE s.token = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public void deleteSession(String token) throws SQLException {
        String sql = "DELETE FROM sessions WHERE token = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.id = rs.getInt("id");
        u.username = rs.getString("username");
        u.passwordHash = rs.getString("password_hash");
        u.role = rs.getString("role");
        u.fullName = rs.getString("full_name");
        u.email = rs.getString("email");
        u.active = rs.getBoolean("active");
        return u;
    }
}
