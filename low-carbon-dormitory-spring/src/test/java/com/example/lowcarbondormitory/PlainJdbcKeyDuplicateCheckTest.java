package com.example.lowcarbondormitory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class PlainJdbcKeyDuplicateCheckTest {

    @Test
    void shouldHaveNoDuplicatePrimaryOrUniqueKeys() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/postgres",
                "postgres",
                "000000")) {
            List<String> findings = new ArrayList<>();
            check(conn, "PRIMARY KEY", "PK", findings);
            check(conn, "UNIQUE", "UK", findings);
            Assertions.assertTrue(findings.isEmpty(), "发现键值重复: " + String.join(" | ", findings));
        }
    }

    private void check(Connection conn, String type, String prefix, List<String> findings) throws Exception {
        String sql = """
                SELECT tc.table_name, tc.constraint_name, kcu.column_name, kcu.ordinal_position
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_name = kcu.constraint_name
                 AND tc.table_schema = kcu.table_schema
                 AND tc.table_name = kcu.table_name
                WHERE tc.table_schema = 'public'
                  AND tc.constraint_type = ?
                ORDER BY tc.table_name, tc.constraint_name, kcu.ordinal_position
                """;
        Map<String, List<String>> constraints = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("table_name") + "::" + rs.getString("constraint_name");
                    constraints.computeIfAbsent(key, k -> new ArrayList<>()).add(rs.getString("column_name"));
                }
            }
        }

        for (Map.Entry<String, List<String>> e : constraints.entrySet()) {
            String table = e.getKey().split("::", 2)[0];
            String constraint = e.getKey().split("::", 2)[1];
            String cols = String.join(", ", e.getValue());
            String dupSql = "SELECT COUNT(*) FROM (SELECT " + cols + " FROM " + table + " GROUP BY " + cols + " HAVING COUNT(*) > 1) t";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(dupSql)) {
                rs.next();
                long count = rs.getLong(1);
                if (count > 0) {
                    findings.add(prefix + ":" + table + "." + constraint + " 重复组数=" + count);
                }
            }
        }
    }
}
