create table if not exists app_user (
    id varchar(80) not null,
    username varchar(120) not null,
    display_name varchar(120),
    email varchar(255),
    status varchar(40) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    unique key uk_app_user_username (username)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists role (
    id varchar(80) not null,
    code varchar(80) not null,
    name varchar(120) not null,
    created_at datetime(6) not null,
    primary key (id),
    unique key uk_role_code (code)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists user_role (
    id varchar(80) not null,
    user_id varchar(80) not null,
    role_id varchar(80) not null,
    created_at datetime(6) not null,
    primary key (id),
    key idx_user_role_user (user_id),
    key idx_user_role_role (role_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists learner_profile (
    id varchar(80) not null,
    learner_id varchar(120) not null,
    target varchar(255) not null,
    weak_points_json text,
    preferences_json text,
    dimensions_json text,
    update_policy varchar(80) not null,
    trace_id varchar(120),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    key idx_learner_profile_learner_updated (learner_id, updated_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists learning_goal (
    id varchar(80) not null,
    learner_id varchar(120) not null,
    title varchar(255) not null,
    description text,
    status varchar(40) not null,
    target_mastery double,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    key idx_learning_goal_learner_status (learner_id, status)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists learning_event (
    id varchar(80) not null,
    learner_id varchar(120) not null,
    event_type varchar(80) not null,
    subject_id varchar(255),
    summary varchar(2000),
    payload_json text,
    trace_id varchar(120),
    created_at datetime(6) not null,
    primary key (id),
    key idx_learning_event_learner_created (learner_id, created_at),
    key idx_learning_event_trace (trace_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists course (
    id varchar(80) not null,
    title varchar(255) not null,
    description text,
    teacher_id varchar(120),
    status varchar(40) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists chapter (
    id varchar(80) not null,
    course_id varchar(80) not null,
    title varchar(255) not null,
    sequence_no integer not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    key idx_chapter_course_seq (course_id, sequence_no)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists knowledge_point (
    id varchar(80) not null,
    course_id varchar(80),
    chapter_id varchar(80),
    title varchar(255) not null,
    description text,
    difficulty double,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    key idx_kp_course_chapter (course_id, chapter_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists knowledge_dependency (
    id varchar(80) not null,
    knowledge_point_id varchar(80) not null,
    prerequisite_id varchar(80) not null,
    dependency_type varchar(40) not null,
    created_at datetime(6) not null,
    primary key (id),
    key idx_kd_point (knowledge_point_id),
    key idx_kd_prereq (prerequisite_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists learning_path (
    id varchar(80) not null,
    learner_id varchar(120) not null,
    goal_id varchar(120) not null,
    reason_summary varchar(2000),
    status varchar(40) not null,
    trace_id varchar(120),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    key idx_learning_path_learner_goal (learner_id, goal_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists learning_path_node (
    id varchar(80) not null,
    path_id varchar(80) not null,
    learner_id varchar(120) not null,
    knowledge_point_id varchar(120) not null,
    title varchar(255) not null,
    status varchar(40) not null,
    mastery double not null,
    reason_summary varchar(2000),
    sequence_no integer not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    key idx_lpn_path_seq (path_id, sequence_no),
    key idx_lpn_learner (learner_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists mastery_record (
    id varchar(80) not null,
    learner_id varchar(120) not null,
    knowledge_point_id varchar(120) not null,
    mastery double not null,
    source_type varchar(80) not null,
    source_id varchar(120),
    reason_summary varchar(2000),
    trace_id varchar(120),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    key idx_mastery_learner_kp_updated (learner_id, knowledge_point_id, updated_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists resource_generation_task (
    id varchar(80) not null,
    learner_id varchar(120) not null,
    goal_id varchar(120) not null,
    path_node_id varchar(120) not null,
    agent_task_id varchar(80) not null,
    status varchar(40) not null,
    review_status varchar(40) not null,
    progress_percent integer not null,
    safety_status varchar(40) not null,
    trace_id varchar(120),
    created_by varchar(120) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    key idx_rgt_learner (learner_id),
    key idx_rgt_agent_task (agent_task_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists learning_resource (
    id varchar(80) not null,
    generation_task_id varchar(80) not null,
    learner_id varchar(120) not null,
    resource_type varchar(80) not null,
    modality varchar(80) not null,
    title varchar(255) not null,
    review_status varchar(40) not null,
    citation_summary varchar(2000),
    markdown_content text not null,
    safety_status varchar(40) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    key idx_learning_resource_task_created (generation_task_id, created_at),
    key idx_learning_resource_learner (learner_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists resource_review (
    id varchar(80) not null,
    generation_task_id varchar(80) not null,
    resource_id varchar(80) not null,
    reviewer_type varchar(80) not null,
    status varchar(40) not null,
    summary varchar(2000),
    trace_id varchar(120),
    created_at datetime(6) not null,
    primary key (id),
    key idx_resource_review_task (generation_task_id),
    key idx_resource_review_resource (resource_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists question (
    id varchar(80) not null,
    course_id varchar(80),
    knowledge_point_id varchar(120),
    question_type varchar(80) not null,
    stem text not null,
    answer_key text,
    difficulty double,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists rubric (
    id varchar(80) not null,
    question_id varchar(80) not null,
    criteria_json text not null,
    max_score double not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    key idx_rubric_question (question_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists answer_record (
    id varchar(80) not null,
    learner_id varchar(120) not null,
    question_id varchar(120) not null,
    answer text not null,
    safety_status varchar(40),
    trace_id varchar(120),
    created_at datetime(6) not null,
    primary key (id),
    key idx_answer_learner_created (learner_id, created_at),
    key idx_answer_question (question_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists grading_result (
    id varchar(80) not null,
    answer_id varchar(80) not null,
    learner_id varchar(120) not null,
    question_id varchar(120) not null,
    score double not null,
    feedback_summary varchar(2000),
    mastery_updates_json text,
    trace_id varchar(120),
    created_at datetime(6) not null,
    primary key (id),
    key idx_grading_answer (answer_id),
    key idx_grading_learner (learner_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists wrong_question (
    id varchar(80) not null,
    learner_id varchar(120) not null,
    question_id varchar(120) not null,
    answer_id varchar(80) not null,
    grading_result_id varchar(80) not null,
    knowledge_point_id varchar(120) not null,
    score double not null,
    cause_analysis varchar(2000),
    resource_push_strategy varchar(1000),
    replan_record_id varchar(120),
    trace_id varchar(120),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    key idx_wrong_question_learner (learner_id),
    key idx_wrong_question_kp (knowledge_point_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists source_citation (
    id varchar(80) not null,
    trace_id varchar(120),
    document_id varchar(80),
    document_name varchar(255),
    page_num integer,
    section_title varchar(255),
    excerpt text,
    score double,
    created_at datetime(6) not null,
    primary key (id),
    key idx_source_citation_trace (trace_id),
    key idx_source_citation_document (document_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists agent_task (
    id varchar(80) not null,
    owner_user_id varchar(120) not null,
    task_type varchar(80) not null,
    status varchar(40) not null,
    input_json text,
    output_json text,
    trace_id varchar(120),
    latency_ms bigint not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    key idx_agent_task_owner (owner_user_id),
    key idx_agent_task_trace (trace_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists agent_trace (
    id varchar(80) not null,
    agent_task_id varchar(80) not null,
    step_id varchar(120) not null,
    agent_name varchar(120) not null,
    status varchar(40) not null,
    summary varchar(2000),
    latency_ms bigint not null,
    model varchar(120),
    prompt_version varchar(120),
    trace_id varchar(120),
    sequence_no integer not null,
    created_at datetime(6) not null,
    primary key (id),
    key idx_agent_trace_task_seq (agent_task_id, sequence_no),
    key idx_agent_trace_trace (trace_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists agent_tool_call (
    id varchar(80) not null,
    agent_task_id varchar(80) not null,
    tool_name varchar(120) not null,
    input_json text,
    output_json text,
    status varchar(40) not null,
    error_message varchar(2000),
    latency_ms bigint,
    created_at datetime(6) not null,
    primary key (id),
    key idx_agent_tool_call_task (agent_task_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists prompt_version (
    id varchar(80) not null,
    code varchar(120) not null,
    version varchar(80) not null,
    prompt_text text not null,
    status varchar(40) not null,
    created_at datetime(6) not null,
    primary key (id),
    unique key uk_prompt_version_code_version (code, version)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists model_call_log (
    id varchar(80) not null,
    trace_id varchar(120) not null,
    agent_task_id varchar(80) not null,
    model varchar(120) not null,
    status varchar(40) not null,
    latency_ms bigint not null,
    error_message varchar(2000),
    estimated_cost double not null,
    created_at datetime(6) not null,
    primary key (id),
    key idx_model_call_trace (trace_id),
    key idx_model_call_task (agent_task_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists token_usage_log (
    id varchar(80) not null,
    trace_id varchar(120) not null,
    agent_task_id varchar(80) not null,
    prompt_tokens integer not null,
    completion_tokens integer not null,
    total_tokens integer not null,
    estimated_cost double not null,
    created_at datetime(6) not null,
    primary key (id),
    key idx_token_usage_trace (trace_id),
    key idx_token_usage_task (agent_task_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists learning_effect_report (
    id varchar(80) not null,
    learner_id varchar(120) not null,
    period_start datetime(6) not null,
    period_end datetime(6) not null,
    mastery_summary_json text,
    activity_score double,
    completion_rate double,
    resource_fit_score double,
    trend_summary varchar(2000),
    created_at datetime(6) not null,
    primary key (id),
    key idx_learning_effect_learner_period (learner_id, period_start, period_end)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
