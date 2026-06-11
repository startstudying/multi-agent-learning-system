DELIMITER //

DROP PROCEDURE IF EXISTS add_column_if_missing//
DROP PROCEDURE IF EXISTS add_index_if_missing//

CREATE PROCEDURE add_column_if_missing(
    IN table_name_value varchar(128),
    IN column_name_value varchar(128),
    IN ddl_value text
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND column_name = column_name_value
    ) THEN
        SET @ddl = ddl_value;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

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

CALL add_column_if_missing(
    'resource_generation_task',
    'request_id',
    'ALTER TABLE resource_generation_task ADD COLUMN request_id varchar(120)'
);

CALL add_index_if_missing(
    'resource_generation_task',
    'uk_rgt_learner_request',
    'CREATE UNIQUE INDEX uk_rgt_learner_request ON resource_generation_task(learner_id, request_id)'
);

DROP PROCEDURE add_index_if_missing;
DROP PROCEDURE add_column_if_missing;
