alter table kb_chat_session
    add column learner_id varchar(120) null,
    add column course_id varchar(120) null,
    add column title varchar(255) null,
    add column status varchar(40) null default 'ACTIVE',
    add column salience_score double null default 0.5,
    add column decay_at datetime(6) null,
    add column created_at datetime(6) null,
    add column updated_at datetime(6) null,
    add column deleted_at datetime(6) null;

alter table kb_chat_message
    add column session_id varchar(80) null,
    add column learner_id varchar(120) null,
    add column role varchar(40) null default 'AI_QA_SUMMARY',
    add column content_summary varchar(2000) null,
    add column source_policy varchar(80) null,
    add column salience_score double null default 0.5,
    add column decay_at datetime(6) null,
    add column editable boolean null default true,
    add column created_at datetime(6) null,
    add column updated_at datetime(6) null,
    add column deleted_at datetime(6) null;

create index idx_kb_chat_message_lifecycle
    on kb_chat_message (learner_id, deleted_at, decay_at, created_at);
