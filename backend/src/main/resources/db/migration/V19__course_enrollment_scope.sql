create table if not exists course_enrollment (
    id varchar(80) primary key,
    course_id varchar(80) not null,
    learner_id varchar(120) not null,
    status varchar(40) not null default 'ACTIVE',
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    unique key uk_course_enrollment_course_learner(course_id, learner_id),
    key idx_course_enrollment_learner_status(learner_id, status),
    key idx_course_enrollment_course_status(course_id, status)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
