package com.kama.jchatmind;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地执行 SQL 迁移脚本（不依赖 psql 命令行）
 */
public class DbSqlMigrationRunner {

    public static void main(String[] args) throws Exception {
        String url = env("JCHATMIND_DB_URL", "jdbc:postgresql://localhost:5432/jchatmind");
        String user = env("JCHATMIND_DB_USER", "postgres");
        String password = env("JCHATMIND_DB_PASSWORD", "123456");

        Path sqlFile = Path.of(args.length > 0 ? args[0] : "user-mail-config-ddl.sql");
        if (!Files.exists(sqlFile)) {
            throw new IllegalArgumentException("SQL 文件不存在: " + sqlFile.toAbsolutePath());
        }

        String ddl = Files.readString(sqlFile);
        List<String> statements = splitSqlStatements(ddl);

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(true);
            try (Statement st = conn.createStatement()) {
                for (String sql : statements) {
                    st.execute(sql);
                    System.out.println("OK: " + firstLine(sql));
                }
            }
            // 验证表是否创建成功
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT COUNT(*) FROM information_schema.tables " +
                                 "WHERE table_schema = 'public' AND table_name = 'user_mail_config'")) {
                rs.next();
                if (rs.getInt(1) != 1) {
                    throw new IllegalStateException("迁移后未找到表 user_mail_config");
                }
            }
        }
        System.out.println("迁移完成: " + sqlFile.getFileName());
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return v != null && !v.isBlank() ? v : defaultValue;
    }

    /** 按分号拆分 SQL，跳过空行与纯注释块 */
    private static List<String> splitSqlStatements(String ddl) {
        List<String> list = new ArrayList<>();
        for (String part : ddl.split(";")) {
            StringBuilder sb = new StringBuilder();
            for (String line : part.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                sb.append(line).append('\n');
            }
            String sql = sb.toString().trim();
            if (!sql.isEmpty()) {
                list.add(sql);
            }
        }
        return list;
    }

    private static String firstLine(String sql) {
        int idx = sql.indexOf('\n');
        String line = idx > 0 ? sql.substring(0, idx) : sql;
        return line.length() > 80 ? line.substring(0, 80) + "..." : line;
    }
}
