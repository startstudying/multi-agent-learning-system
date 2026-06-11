DELIMITER //

DROP PROCEDURE IF EXISTS add_column_if_missing//

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

DELIMITER ;

CALL add_column_if_missing(
    'learning_path_node',
    'recommendation_reason',
    'ALTER TABLE learning_path_node ADD COLUMN recommendation_reason varchar(2000)'
);

CALL add_column_if_missing(
    'learning_path_node',
    'estimated_duration_minutes',
    'ALTER TABLE learning_path_node ADD COLUMN estimated_duration_minutes integer'
);

CALL add_column_if_missing(
    'learning_path_node',
    'resource_type',
    'ALTER TABLE learning_path_node ADD COLUMN resource_type varchar(80)'
);

CALL add_column_if_missing(
    'learning_path_node',
    'assessment_binding_relation',
    'ALTER TABLE learning_path_node ADD COLUMN assessment_binding_relation varchar(255)'
);

DROP PROCEDURE add_column_if_missing;
