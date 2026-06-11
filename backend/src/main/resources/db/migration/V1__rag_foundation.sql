create table if not exists kb_knowledge_base (
    id varchar(80) not null,
    name varchar(255) not null,
    description varchar(1000),
    visibility varchar(32) not null,
    owner_user_id varchar(120) not null,
    created_by varchar(120) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    deleted_at datetime(6),
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists kb_permission (
    id varchar(80) not null,
    kb_id varchar(80) not null,
    subject_type varchar(40) not null,
    subject_id varchar(120) not null,
    permission varchar(40) not null,
    created_at datetime(6) not null,
    primary key (id),
    constraint fk_kb_permission_kb foreign key (kb_id) references kb_knowledge_base(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists kb_document (
    id varchar(80) not null,
    kb_id varchar(80) not null,
    course_id varchar(120),
    chapter_id varchar(120),
    name varchar(255) not null,
    content_type varchar(255),
    size_bytes bigint,
    storage_bucket varchar(255),
    storage_key varchar(1000),
    version integer not null default 1,
    parse_status varchar(40) not null,
    index_status varchar(40) not null,
    error_message varchar(2000),
    created_by varchar(120) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    deleted_at datetime(6),
    primary key (id),
    constraint fk_kb_document_kb foreign key (kb_id) references kb_knowledge_base(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists kb_doc_chunk (
    id varchar(80) not null,
    kb_id varchar(80) not null,
    document_id varchar(80) not null,
    document_version integer not null,
    chunk_index integer not null,
    content text not null,
    page_num integer,
    section_title varchar(255),
    metadata_json varchar(4000),
    created_at datetime(6) not null,
    primary key (id),
    constraint fk_kb_doc_chunk_document foreign key (document_id) references kb_document(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists kb_index_task (
    id varchar(80) not null,
    document_id varchar(80) not null,
    kb_id varchar(80) not null,
    status varchar(40) not null,
    retry_count integer not null default 0,
    error_message varchar(2000),
    started_at datetime(6),
    finished_at datetime(6),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    constraint fk_kb_index_task_document foreign key (document_id) references kb_document(id),
    constraint fk_kb_index_task_kb foreign key (kb_id) references kb_knowledge_base(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists kb_chat_session (
    id varchar(80) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists kb_chat_message (
    id varchar(80) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists kb_query_log (
    id varchar(80) not null,
    trace_id varchar(120),
    user_id varchar(120),
    kb_ids_json text,
    question text,
    retrieval_count integer,
    reranker_status varchar(80),
    sources_json text,
    latency_ms bigint,
    created_at datetime(6),
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_kb_permission_subject on kb_permission(subject_type, subject_id);
create index idx_kb_document_kb_status_deleted on kb_document(kb_id, index_status, deleted_at);
create index idx_kb_doc_chunk_kb_doc on kb_doc_chunk(kb_id, document_id);
create index idx_kb_index_task_status on kb_index_task(status, created_at);
create index idx_kb_query_log_trace_id on kb_query_log(trace_id);

create table if not exists learner_profile (
    id varchar(80) not null,
    learner_id varchar(255) not null,
    target varchar(255) not null,
    weak_points_json varchar(4000),
    preferences_json varchar(4000),
    dimensions_json text,
    update_policy varchar(255) not null,
    trace_id varchar(255),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists learning_event (
    id varchar(80) not null,
    learner_id varchar(255) not null,
    event_type varchar(255) not null,
    subject_id varchar(255),
    summary varchar(2000),
    payload_json text,
    trace_id varchar(255),
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists learning_path (
    id varchar(80) not null,
    learner_id varchar(255) not null,
    goal_id varchar(255) not null,
    reason_summary varchar(2000),
    status varchar(255) not null,
    trace_id varchar(255),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists learning_path_node (
    id varchar(80) not null,
    path_id varchar(255) not null,
    learner_id varchar(255) not null,
    knowledge_point_id varchar(255) not null,
    title varchar(255) not null,
    status varchar(255) not null,
    mastery double not null,
    reason_summary varchar(2000),
    sequence_no integer not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists mastery_record (
    id varchar(80) not null,
    learner_id varchar(255) not null,
    knowledge_point_id varchar(255) not null,
    mastery double not null,
    source_type varchar(255) not null,
    source_id varchar(255),
    reason_summary varchar(2000),
    trace_id varchar(255),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists answer_record (
    id varchar(80) not null,
    learner_id varchar(255) not null,
    question_id varchar(255) not null,
    answer text not null,
    safety_status varchar(255),
    trace_id varchar(255),
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists grading_result (
    id varchar(80) not null,
    answer_id varchar(255) not null,
    learner_id varchar(255) not null,
    question_id varchar(255) not null,
    score double not null,
    feedback_summary varchar(2000),
    mastery_updates_json text,
    trace_id varchar(255),
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists wrong_question (
    id varchar(80) not null,
    learner_id varchar(255) not null,
    question_id varchar(255) not null,
    answer_id varchar(255) not null,
    grading_result_id varchar(255) not null,
    knowledge_point_id varchar(255) not null,
    score double not null,
    cause_analysis varchar(2000),
    resource_push_strategy varchar(1000),
    replan_record_id varchar(255),
    trace_id varchar(255),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists agent_task (
    id varchar(80) not null,
    owner_user_id varchar(255) not null,
    task_type varchar(255) not null,
    status varchar(255) not null,
    input_json text,
    output_json text,
    trace_id varchar(255),
    latency_ms bigint not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists agent_trace (
    id varchar(80) not null,
    agent_task_id varchar(255) not null,
    step_id varchar(255) not null,
    agent_name varchar(255) not null,
    status varchar(255) not null,
    summary varchar(2000),
    latency_ms bigint not null,
    model varchar(255),
    prompt_version varchar(255),
    trace_id varchar(255),
    sequence_no integer not null,
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists learning_resource (
    id varchar(80) not null,
    generation_task_id varchar(255) not null,
    learner_id varchar(255) not null,
    resource_type varchar(255) not null,
    modality varchar(255) not null,
    title varchar(255) not null,
    review_status varchar(255) not null,
    citation_summary varchar(2000),
    markdown_content text not null,
    safety_status varchar(255) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists model_call_log (
    id varchar(80) not null,
    trace_id varchar(255) not null,
    agent_task_id varchar(255) not null,
    model varchar(255) not null,
    status varchar(255) not null,
    latency_ms bigint not null,
    error_message varchar(2000),
    estimated_cost double not null,
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists resource_generation_task (
    id varchar(80) not null,
    learner_id varchar(255) not null,
    goal_id varchar(255) not null,
    path_node_id varchar(255) not null,
    agent_task_id varchar(255) not null,
    status varchar(255) not null,
    review_status varchar(255) not null,
    progress_percent integer not null,
    safety_status varchar(255) not null,
    trace_id varchar(255),
    created_by varchar(255) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists resource_review (
    id varchar(80) not null,
    generation_task_id varchar(255) not null,
    resource_id varchar(255) not null,
    reviewer_type varchar(255) not null,
    status varchar(255) not null,
    summary varchar(2000),
    trace_id varchar(255),
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists token_usage_log (
    id varchar(80) not null,
    trace_id varchar(255) not null,
    agent_task_id varchar(255) not null,
    prompt_tokens integer not null,
    completion_tokens integer not null,
    total_tokens integer not null,
    estimated_cost double not null,
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_learner_profile_learner_updated on learner_profile(learner_id, updated_at);
create index idx_learning_event_learner on learning_event(learner_id);
create index idx_learning_path_node_path_sequence on learning_path_node(path_id, sequence_no);
create index idx_mastery_record_lookup on mastery_record(learner_id, knowledge_point_id, updated_at);
create index idx_answer_record_learner on answer_record(learner_id);
create index idx_wrong_question_learner on wrong_question(learner_id);
create index idx_agent_trace_task_sequence on agent_trace(agent_task_id, sequence_no);
create index idx_learning_resource_task_created on learning_resource(generation_task_id, created_at);
create index idx_model_call_log_trace on model_call_log(trace_id);
create index idx_resource_review_task on resource_review(generation_task_id);
create index idx_token_usage_log_trace on token_usage_log(trace_id);
