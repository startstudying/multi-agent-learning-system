DELIMITER //

DROP PROCEDURE IF EXISTS add_index_if_missing//

CREATE PROCEDURE add_index_if_missing(
    IN table_name_value varchar(128),
    IN index_name_value varchar(128),
    IN ddl_value text
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND index_name = index_name_value
    ) THEN
        SET @ddl = ddl_value;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL add_index_if_missing(
    'learning_event',
    'idx_learning_event_learner_created',
    'CREATE INDEX idx_learning_event_learner_created ON learning_event(learner_id, created_at)'
);
CALL add_index_if_missing(
    'learning_event',
    'idx_learning_event_trace',
    'CREATE INDEX idx_learning_event_trace ON learning_event(trace_id)'
);
CALL add_index_if_missing(
    'learning_path',
    'idx_learning_path_learner_goal',
    'CREATE INDEX idx_learning_path_learner_goal ON learning_path(learner_id, goal_id)'
);
CALL add_index_if_missing(
    'answer_record',
    'idx_answer_learner_created',
    'CREATE INDEX idx_answer_learner_created ON answer_record(learner_id, created_at)'
);
CALL add_index_if_missing(
    'answer_record',
    'idx_answer_question',
    'CREATE INDEX idx_answer_question ON answer_record(question_id)'
);
CALL add_index_if_missing(
    'grading_result',
    'idx_grading_answer',
    'CREATE INDEX idx_grading_answer ON grading_result(answer_id)'
);
CALL add_index_if_missing(
    'grading_result',
    'idx_grading_learner',
    'CREATE INDEX idx_grading_learner ON grading_result(learner_id)'
);
CALL add_index_if_missing(
    'wrong_question',
    'idx_wrong_question_kp',
    'CREATE INDEX idx_wrong_question_kp ON wrong_question(knowledge_point_id)'
);
CALL add_index_if_missing(
    'resource_generation_task',
    'idx_rgt_learner',
    'CREATE INDEX idx_rgt_learner ON resource_generation_task(learner_id)'
);
CALL add_index_if_missing(
    'resource_generation_task',
    'idx_rgt_agent_task',
    'CREATE INDEX idx_rgt_agent_task ON resource_generation_task(agent_task_id)'
);
CALL add_index_if_missing(
    'learning_resource',
    'idx_learning_resource_learner',
    'CREATE INDEX idx_learning_resource_learner ON learning_resource(learner_id)'
);
CALL add_index_if_missing(
    'resource_review',
    'idx_resource_review_resource',
    'CREATE INDEX idx_resource_review_resource ON resource_review(resource_id)'
);
CALL add_index_if_missing(
    'agent_task',
    'idx_agent_task_owner',
    'CREATE INDEX idx_agent_task_owner ON agent_task(owner_user_id)'
);
CALL add_index_if_missing(
    'agent_task',
    'idx_agent_task_trace',
    'CREATE INDEX idx_agent_task_trace ON agent_task(trace_id)'
);
CALL add_index_if_missing(
    'agent_trace',
    'idx_agent_trace_trace',
    'CREATE INDEX idx_agent_trace_trace ON agent_trace(trace_id)'
);
CALL add_index_if_missing(
    'model_call_log',
    'idx_model_call_task',
    'CREATE INDEX idx_model_call_task ON model_call_log(agent_task_id)'
);
CALL add_index_if_missing(
    'token_usage_log',
    'idx_token_usage_task',
    'CREATE INDEX idx_token_usage_task ON token_usage_log(agent_task_id)'
);

DROP PROCEDURE add_index_if_missing;
