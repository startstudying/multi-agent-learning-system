-- Demo seed data for local development and smoke testing.
-- This migration is safe to re-run because it uses INSERT IGNORE.

INSERT IGNORE INTO app_user (id, username, display_name, email, status, created_at, updated_at)
VALUES ('stu_001', 'student001', 'Demo Student', 'student001@example.com', 'ACTIVE', NOW(6), NOW(6));

INSERT IGNORE INTO app_user (id, username, display_name, email, status, created_at, updated_at)
VALUES ('tch_001', 'teacher001', 'Demo Teacher', 'teacher001@example.com', 'ACTIVE', NOW(6), NOW(6));

INSERT IGNORE INTO app_user (id, username, display_name, email, status, created_at, updated_at)
VALUES ('adm_001', 'admin001', 'Demo Admin', 'admin001@example.com', 'ACTIVE', NOW(6), NOW(6));

INSERT IGNORE INTO role (id, code, name, created_at) VALUES ('role_student', 'STUDENT', 'Student', NOW(6));
INSERT IGNORE INTO role (id, code, name, created_at) VALUES ('role_teacher', 'TEACHER', 'Teacher', NOW(6));
INSERT IGNORE INTO role (id, code, name, created_at) VALUES ('role_admin', 'ADMIN', 'Admin', NOW(6));

INSERT IGNORE INTO user_role (id, user_id, role_id, created_at) VALUES ('ur_stu', 'stu_001', 'role_student', NOW(6));
INSERT IGNORE INTO user_role (id, user_id, role_id, created_at) VALUES ('ur_tch', 'tch_001', 'role_teacher', NOW(6));
INSERT IGNORE INTO user_role (id, user_id, role_id, created_at) VALUES ('ur_adm', 'adm_001', 'role_admin', NOW(6));

-- Demo knowledge base: Java Backend
INSERT IGNORE INTO kb_knowledge_base (id, name, description, visibility, owner_user_id, created_by, created_at, updated_at)
VALUES ('kb_java_backend', 'Java Backend Course', 'Demo knowledge base for Java backend learning materials', 'PUBLIC', 'tch_001', 'tch_001', NOW(6), NOW(6));

-- Grant READ permission to the demo student
INSERT IGNORE INTO kb_permission (id, kb_id, subject_type, subject_id, permission, created_at)
VALUES ('kb_perm_stu_001_read', 'kb_java_backend', 'USER', 'stu_001', 'READ', NOW(6));

-- Grant OWNER permission to the demo teacher
INSERT IGNORE INTO kb_permission (id, kb_id, subject_type, subject_id, permission, created_at)
VALUES ('kb_perm_tch_001_owner', 'kb_java_backend', 'USER', 'tch_001', 'OWNER', NOW(6));
