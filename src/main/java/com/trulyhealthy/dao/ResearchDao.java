package com.trulyhealthy.dao;

import com.trulyhealthy.config.DBConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only, anonymized statistics for the Research/Government official role.
 * Deliberately never selects names, contacts, addresses, or free-text notes —
 * only counts and category breakdowns, so no individual patient is identifiable.
 */
public class ResearchDao {

    public Map<String, Object> summary() throws SQLException {
        Map<String, Object> out = new LinkedHashMap<>();
        try (Connection c = DBConfig.getConnection()) {
            out.put("totalPatients", scalar(c, "SELECT COUNT(*) FROM patients"));
            out.put("totalDoctors", scalar(c, "SELECT COUNT(*) FROM doctors"));
            out.put("totalAppointments", scalar(c, "SELECT COUNT(*) FROM appointments"));
            out.put("completedAppointments", scalar(c, "SELECT COUNT(*) FROM appointments WHERE status='COMPLETED'"));
            out.put("cancelledAppointments", scalar(c, "SELECT COUNT(*) FROM appointments WHERE status='CANCELLED'"));
            out.put("genderBreakdown", keyCount(c,
                    "SELECT COALESCE(gender,'Unspecified') g, COUNT(*) FROM patients GROUP BY g"));
            out.put("bloodGroupBreakdown", keyCount(c,
                    "SELECT COALESCE(blood_group,'Unspecified') b, COUNT(*) FROM patients GROUP BY b"));
            out.put("specializationLoad", keyCount(c,
                    "SELECT COALESCE(specialization,'General') s, COUNT(*) FROM doctors GROUP BY s"));
            out.put("topDiagnoses", keyCount(c,
                    "SELECT diagnosis, COUNT(*) c FROM medical_records WHERE diagnosis IS NOT NULL " +
                            "GROUP BY diagnosis ORDER BY c DESC LIMIT 10"));
        }
        return out;
    }

    private long scalar(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private List<Map<String, Object>> keyCount(Connection c, String sql) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("label", rs.getString(1));
                row.put("count", rs.getLong(2));
                out.add(row);
            }
        }
        return out;
    }
}
