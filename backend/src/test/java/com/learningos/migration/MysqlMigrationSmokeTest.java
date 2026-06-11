package com.learningos.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "learningos.mysql.smoke", matches = "true")
class MysqlMigrationSmokeTest {

    private static final String DEFAULT_URL = "jdbc:mysql://127.0.0.1:3306/learning_os_migration_smoke"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String LATEST_MIGRATION_VERSION = "22";
    private static final int LATEST_MIGRATION_COUNT = 22;

    @Test
    void migratesEmptyMysqlSchemaThroughLatestVersionAndVerifiesMysqlDialectObjects() {
        SmokeConfig config = SmokeConfig.fromSystemProperties();
        assertSmokeSchemaName(config.schemaName());

        try {
            recreateSchema(config);

            Flyway flyway = Flyway.configure()
                    .dataSource(config.schemaUrl(), config.username(), config.password())
                    .locations("classpath:db/migration")
                    .load();

            MigrateResult result = flyway.migrate();
            MigrationInfo current = flyway.info().current();

            assertThat(result.migrationsExecuted).isEqualTo(LATEST_MIGRATION_COUNT);
            assertThat(current).isNotNull();
            assertThat(current.getVersion().getVersion()).isEqualTo(LATEST_MIGRATION_VERSION);

            verifyMigratedSchema(config);
        } catch (SQLException | RuntimeException ex) {
            throw new AssertionError("""
                    MySQL migration smoke failed. Ensure MySQL 8 is reachable, or run:
                    docker compose -f backend/docker-compose.yml up -d mysql
                    Then execute the smoke command with -Dlearningos.mysql.smoke=true.
                    URL: %s
                    """.formatted(config.redactedSchemaUrl()), ex);
        } finally {
            dropSchemaIfRequested(config);
        }
    }

    private static void verifyMigratedSchema(SmokeConfig config) throws SQLException {
        try (Connection connection = DriverManager.getConnection(config.schemaUrl(), config.username(), config.password())) {
            String version = queryString(connection, "select version()");
            assertThat(version)
                    .as("P3-1 requires MySQL 8.x, not H2 or another MySQL-compatible engine")
                    .startsWith("8.");

            assertThat(tableExists(connection, "kb_knowledge_base")).isTrue();
            assertThat(tableExists(connection, "app_user")).isTrue();
            assertThat(tableExists(connection, "answer_record")).isTrue();
            assertThat(tableExists(connection, "resource_generation_task")).isTrue();

            assertThat(columnExists(connection, "resource_generation_task", "request_id")).isTrue();
            assertThat(indexExists(connection, "resource_generation_task", "uk_rgt_learner_request")).isTrue();
            assertThat(columnExists(connection, "answer_record", "request_id")).isTrue();
            assertThat(columnExists(connection, "answer_record", "request_hash")).isTrue();
            assertThat(columnExists(connection, "answer_record", "response_json")).isTrue();
            assertThat(indexExists(connection, "answer_record", "uk_answer_learner_request")).isTrue();
            assertThat(indexExists(connection, "agent_task", "idx_agent_task_owner")).isTrue();

            assertThat(tableEngine(connection, "answer_record")).isEqualToIgnoringCase("InnoDB");
            assertThat(tableCollation(connection, "answer_record")).startsWith("utf8mb4");
            assertThat(columnType(connection, "answer_record", "response_json")).isEqualToIgnoringCase("text");
            assertThat(columnType(connection, "kb_query_log", "kb_ids_json")).isEqualToIgnoringCase("text");
            assertThat(columnType(connection, "kb_query_log", "question")).isEqualToIgnoringCase("text");
            assertThat(columnType(connection, "kb_query_log", "sources_json")).isEqualToIgnoringCase("text");

            assertThat(routineExists(connection, "add_column_if_missing")).isFalse();
            assertThat(routineExists(connection, "add_index_if_missing")).isFalse();

            assertThat(queryLong(connection, """
                    select count(*)
                    from flyway_schema_history
                    where success = 1
                    """)).isEqualTo(LATEST_MIGRATION_COUNT);
            assertThat(queryLong(connection, """
                    select count(*)
                    from flyway_schema_history
                    where success = 1 and version in ('1', '2', '3', '4', '5')
                    """)).isEqualTo(5L);
            assertThat(queryLong(connection, """
                    select count(*)
                    from flyway_schema_history
                    where success = 1 and version in ('6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22')
                    """)).isEqualTo(17L);

            verifyCurrentMigrationObjects(connection);
        }
    }

    private static void verifyCurrentMigrationObjects(Connection connection) throws SQLException {
        assertThat(columnExists(connection, "resource_review", "reason")).isTrue();
        assertThat(columnExists(connection, "resource_review", "citation_check")).isTrue();
        assertThat(columnExists(connection, "resource_review", "safety_check")).isTrue();
        assertThat(columnExists(connection, "resource_review", "revision_suggestion")).isTrue();

        assertThat(columnExists(connection, "kb_query_log", "request_id")).isTrue();
        assertThat(columnExists(connection, "kb_query_log", "request_hash")).isTrue();
        assertThat(columnExists(connection, "kb_query_log", "response_json")).isTrue();
        assertThat(columnType(connection, "kb_query_log", "response_json")).isEqualToIgnoringCase("text");
        assertThat(indexExists(connection, "kb_query_log", "uk_kb_query_user_request")).isTrue();

        assertThat(columnExists(connection, "kb_document", "request_id")).isTrue();
        assertThat(columnExists(connection, "kb_document", "request_hash")).isTrue();
        assertThat(columnExists(connection, "kb_document", "response_json")).isTrue();
        assertThat(indexExists(connection, "kb_document", "uk_kb_document_user_request")).isTrue();

        assertThat(columnExists(connection, "learning_path_node", "recommendation_reason")).isTrue();
        assertThat(columnExists(connection, "learning_path_node", "estimated_duration_minutes")).isTrue();
        assertThat(columnExists(connection, "learning_path_node", "resource_type")).isTrue();
        assertThat(columnExists(connection, "learning_path_node", "assessment_binding_relation")).isTrue();

        assertThat(columnExists(connection, "learning_path", "profile_snapshot")).isTrue();
        assertThat(columnExists(connection, "resource_generation_task", "profile_snapshot")).isTrue();
        assertThat(columnExists(connection, "resource_generation_task", "retry_count")).isTrue();
        assertThat(columnExists(connection, "resource_generation_task", "next_retry_at")).isTrue();
        assertThat(columnExists(connection, "resource_generation_task", "last_error")).isTrue();
        assertThat(columnExists(connection, "resource_generation_task", "recoverable")).isTrue();

        assertThat(columnExists(connection, "model_call_log", "prompt_code")).isTrue();
        assertThat(columnExists(connection, "model_call_log", "prompt_version")).isTrue();
        assertThat(columnExists(connection, "model_call_log", "temperature")).isTrue();
        assertThat(columnExists(connection, "model_call_log", "structured_output_schema")).isTrue();
        assertThat(columnExists(connection, "model_call_log", "provider")).isTrue();
        assertThat(columnType(connection, "model_call_log", "provider")).isEqualToIgnoringCase("varchar(80)");
        assertThat(columnExists(connection, "course_enrollment", "id")).isTrue();
        assertThat(columnExists(connection, "course_enrollment", "course_id")).isTrue();
        assertThat(columnExists(connection, "course_enrollment", "learner_id")).isTrue();
        assertThat(columnExists(connection, "course_enrollment", "status")).isTrue();
        assertThat(columnType(connection, "course_enrollment", "course_id")).isEqualToIgnoringCase("varchar(80)");
        assertThat(columnType(connection, "course_enrollment", "learner_id")).isEqualToIgnoringCase("varchar(120)");
        assertThat(columnType(connection, "course_enrollment", "status")).isEqualToIgnoringCase("varchar(40)");
        java.util.Set<String> enrollmentIndexes = new java.util.HashSet<>();
        try (var indexes = connection.getMetaData()
                .getIndexInfo(connection.getCatalog(), null, "course_enrollment", false, false)) {
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if (indexName != null) {
                    enrollmentIndexes.add(indexName);
                }
            }
        }
        assertThat(enrollmentIndexes)
                .contains(
                        "uk_course_enrollment_course_learner",
                        "idx_course_enrollment_learner_status",
                        "idx_course_enrollment_course_status"
                );

        assertThat(tableExists(connection, "evaluation_set")).isTrue();
        assertThat(tableExists(connection, "evaluation_sample")).isTrue();
        assertThat(indexExists(connection, "evaluation_set", "uk_evaluation_set_owner_code_version")).isTrue();
        assertThat(indexExists(connection, "evaluation_sample", "idx_evaluation_sample_set")).isTrue();
        assertThat(checkConstraintExists(connection, "evaluation_set", "ck_evaluation_set_type")).isTrue();
        assertThat(foreignKeyExists(connection, "evaluation_sample", "fk_evaluation_sample_set")).isTrue();

        assertThat(tableExists(connection, "evaluation_run")).isTrue();
        assertThat(tableExists(connection, "evaluation_run_metric")).isTrue();
        assertThat(indexExists(connection, "evaluation_run", "idx_evaluation_run_set_prompt")).isTrue();
        assertThat(indexExists(connection, "evaluation_run_metric", "uk_evaluation_run_metric_run_name")).isTrue();
        assertThat(checkConstraintExists(connection, "evaluation_run", "ck_evaluation_run_status")).isTrue();
        assertThat(checkConstraintExists(connection, "evaluation_run_metric", "ck_evaluation_run_metric_sample_count")).isTrue();
        assertThat(foreignKeyExists(connection, "evaluation_run", "fk_evaluation_run_set")).isTrue();
        assertThat(foreignKeyExists(connection, "evaluation_run_metric", "fk_evaluation_run_metric_run")).isTrue();

        assertThat(columnExists(connection, "agent_tool_call", "trace_id")).isTrue();
        assertThat(columnExists(connection, "agent_tool_call", "input_summary")).isTrue();
        assertThat(columnExists(connection, "agent_tool_call", "output_summary")).isTrue();
        assertThat(columnExists(connection, "agent_tool_call", "retention_class")).isTrue();
        assertThat(indexExists(connection, "agent_tool_call", "idx_agent_tool_call_trace")).isTrue();
        assertThat(indexExists(connection, "agent_tool_call", "idx_agent_tool_call_status")).isTrue();

        assertThat(columnExists(connection, "kb_index_task", "progress_percent")).isTrue();
        assertThat(columnExists(connection, "kb_index_task", "progress_phase")).isTrue();
        assertThat(columnExists(connection, "kb_index_task", "heartbeat_at")).isTrue();
        assertThat(columnExists(connection, "kb_index_task", "lease_owner")).isTrue();
        assertThat(columnExists(connection, "kb_index_task", "lease_until")).isTrue();
        assertThat(columnExists(connection, "kb_index_task", "next_retry_at")).isTrue();
        assertThat(columnExists(connection, "kb_index_task", "recoverable")).isTrue();
        assertThat(indexExists(connection, "kb_index_task", "idx_kb_index_task_due")).isTrue();
        assertThat(indexExists(connection, "kb_index_task", "idx_kb_index_task_lease")).isTrue();

        assertThat(columnExists(connection, "kb_doc_chunk", "chunk_hash")).isTrue();
        assertThat(indexExists(connection, "kb_doc_chunk", "uk_kb_doc_chunk_document_version_hash")).isTrue();
        assertThat(indexExists(connection, "kb_doc_chunk", "idx_kb_doc_chunk_document_version_hash")).isTrue();

        assertThat(columnExists(connection, "kb_knowledge_base", "course_id")).isTrue();
        assertThat(columnExists(connection, "kb_knowledge_base", "binding_status")).isTrue();
        assertThat(columnExists(connection, "kb_knowledge_base", "bound_by")).isTrue();
        assertThat(columnExists(connection, "kb_knowledge_base", "bound_at")).isTrue();
        assertThat(columnType(connection, "kb_knowledge_base", "course_id")).isEqualToIgnoringCase("varchar(80)");
        assertThat(columnType(connection, "kb_knowledge_base", "binding_status")).isEqualToIgnoringCase("varchar(40)");
        assertThat(indexExists(connection, "kb_knowledge_base", "idx_kb_course_binding")).isTrue();
        assertThat(indexExists(connection, "kb_document", "idx_kb_document_kb_course_deleted")).isTrue();
        assertThat(checkConstraintExists(connection, "kb_knowledge_base", "ck_kb_binding_status")).isTrue();
        assertThat(checkConstraintExists(connection, "kb_knowledge_base", "ck_kb_binding_course_consistency")).isTrue();
        assertThat(foreignKeyExists(connection, "kb_knowledge_base", "fk_kb_course_binding_course")).isTrue();

        assertThat(tableExists(connection, "model_provider")).isTrue();
        assertThat(columnExists(connection, "model_provider", "provider_code")).isTrue();
        assertThat(columnExists(connection, "model_provider", "api_key_ciphertext")).isTrue();
        assertThat(indexExists(connection, "model_provider", "uk_model_provider_code")).isTrue();
        assertThat(indexExists(connection, "model_provider", "idx_model_provider_default_enabled")).isTrue();

        assertThat(tableExists(connection, "ops_alert_record")).isTrue();
        assertThat(columnExists(connection, "ops_alert_record", "alert_type")).isTrue();
        assertThat(columnExists(connection, "ops_alert_record", "notification_status")).isTrue();
        assertThat(indexExists(connection, "ops_alert_record", "uk_ops_alert_window")).isTrue();
        assertThat(indexExists(connection, "ops_alert_record", "idx_ops_alert_status")).isTrue();

        assertThat(routineExists(connection, "add_column_if_missing")).isFalse();
        assertThat(routineExists(connection, "add_index_if_missing")).isFalse();
    }

    private static void recreateSchema(SmokeConfig config) throws SQLException {
        try (Connection connection = DriverManager.getConnection(config.serverUrl(), config.username(), config.password());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + config.schemaName() + "`");
            statement.execute("CREATE DATABASE `" + config.schemaName()
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
    }

    private static void dropSchemaIfRequested(SmokeConfig config) {
        if (config.keepSchema()) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(config.serverUrl(), config.username(), config.password());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + config.schemaName() + "`");
        } catch (SQLException ignored) {
            // Do not hide the original migration failure with cleanup errors.
        }
    }

    private static void assertSmokeSchemaName(String schemaName) {
        assertThat(schemaName)
                .as("Smoke schema names are restricted to avoid destructive cleanup of non-test databases")
                .matches("[A-Za-z0-9_]+");

        boolean allowNonSmokeSchema = Boolean.getBoolean("learningos.mysql.smoke.allowNonSmokeSchema");
        if (!allowNonSmokeSchema) {
            assertThat(schemaName.toLowerCase(Locale.ROOT))
                    .as("Schema name must contain migration_smoke unless learningos.mysql.smoke.allowNonSmokeSchema=true")
                    .contains("migration_smoke");
        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        return exists(connection, """
                select 1
                from information_schema.tables
                where table_schema = database() and table_name = ?
                """, tableName);
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        return exists(connection, """
                select 1
                from information_schema.columns
                where table_schema = database() and table_name = ? and column_name = ?
                """, tableName, columnName);
    }

    private static boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        return exists(connection, """
                select 1
                from information_schema.statistics
                where table_schema = database() and table_name = ? and index_name = ?
                """, tableName, indexName);
    }

    private static boolean routineExists(Connection connection, String routineName) throws SQLException {
        return exists(connection, """
                select 1
                from information_schema.routines
                where routine_schema = database() and routine_name = ?
                """, routineName);
    }

    private static boolean checkConstraintExists(Connection connection, String tableName, String constraintName) throws SQLException {
        return exists(connection, """
                select 1
                from information_schema.check_constraints
                where constraint_schema = database() and constraint_name = ?
                """, constraintName)
                || exists(connection, """
                select 1
                from information_schema.table_constraints
                where table_schema = database()
                  and table_name = ?
                  and constraint_name = ?
                  and constraint_type = 'CHECK'
                """, tableName, constraintName);
    }

    private static boolean foreignKeyExists(Connection connection, String tableName, String constraintName) throws SQLException {
        return exists(connection, """
                select 1
                from information_schema.table_constraints
                where table_schema = database()
                  and table_name = ?
                  and constraint_name = ?
                  and constraint_type = 'FOREIGN KEY'
                """, tableName, constraintName);
    }

    private static String tableEngine(Connection connection, String tableName) throws SQLException {
        return queryString(connection, """
                select engine
                from information_schema.tables
                where table_schema = database() and table_name = ?
                """, tableName);
    }

    private static String tableCollation(Connection connection, String tableName) throws SQLException {
        return queryString(connection, """
                select table_collation
                from information_schema.tables
                where table_schema = database() and table_name = ?
                """, tableName);
    }

    private static String columnType(Connection connection, String tableName, String columnName) throws SQLException {
        return queryString(connection, """
                select column_type
                from information_schema.columns
                where table_schema = database() and table_name = ? and column_name = ?
                """, tableName, columnName);
    }

    private static boolean exists(Connection connection, String sql, String... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                statement.setString(i + 1, values[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static String queryString(Connection connection, String sql, String... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                statement.setString(i + 1, values[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).as(sql).isTrue();
                return resultSet.getString(1);
            }
        }
    }

    private static long queryLong(Connection connection, String sql, String... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                statement.setString(i + 1, values[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).as(sql).isTrue();
                return resultSet.getLong(1);
            }
        }
    }

    private record SmokeConfig(
            String schemaUrl,
            String serverUrl,
            String schemaName,
            String username,
            String password,
            boolean keepSchema
    ) {
        static SmokeConfig fromSystemProperties() {
            String schemaUrl = System.getProperty("learningos.mysql.smoke.url", DEFAULT_URL);
            JdbcMysqlUrl parsed = JdbcMysqlUrl.parse(schemaUrl);
            return new SmokeConfig(
                    parsed.schemaUrl(),
                    parsed.serverUrl(),
                    parsed.schemaName(),
                    System.getProperty("learningos.mysql.smoke.username", "root"),
                    System.getProperty("learningos.mysql.smoke.password", "learning_os_root"),
                    Boolean.getBoolean("learningos.mysql.smoke.keepSchema")
            );
        }

        String redactedSchemaUrl() {
            return schemaUrl;
        }
    }

    private record JdbcMysqlUrl(String schemaUrl, String serverUrl, String schemaName) {
        private static final String PREFIX = "jdbc:mysql://";

        static JdbcMysqlUrl parse(String url) {
            if (!url.startsWith(PREFIX)) {
                throw new IllegalArgumentException("Only jdbc:mysql:// URLs are supported for migration smoke tests");
            }

            int pathStart = url.indexOf('/', PREFIX.length());
            if (pathStart < 0) {
                throw new IllegalArgumentException("MySQL smoke URL must include a schema name");
            }

            int queryStart = url.indexOf('?', pathStart);
            String hostPart = url.substring(0, pathStart);
            String schemaName = queryStart < 0
                    ? url.substring(pathStart + 1)
                    : url.substring(pathStart + 1, queryStart);
            String queryPart = queryStart < 0 ? "" : url.substring(queryStart);

            if (schemaName.isBlank()) {
                throw new IllegalArgumentException("MySQL smoke URL must include a non-empty schema name");
            }

            return new JdbcMysqlUrl(hostPart + "/" + schemaName + queryPart, hostPart + "/" + queryPart, schemaName);
        }
    }
}
