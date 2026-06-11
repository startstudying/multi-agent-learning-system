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
    'learning_path',
    'profile_snapshot',
    'ALTER TABLE learning_path ADD COLUMN profile_snapshot text'
);

CALL add_column_if_missing(
    'resource_generation_task',
    'profile_snapshot',
    'ALTER TABLE resource_generation_task ADD COLUMN profile_snapshot text'
);

DROP PROCEDURE add_column_if_missing;
