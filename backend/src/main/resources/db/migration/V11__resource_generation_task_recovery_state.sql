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
    'resource_generation_task',
    'retry_count',
    'ALTER TABLE resource_generation_task ADD COLUMN retry_count int NOT NULL DEFAULT 0'
);

CALL add_column_if_missing(
    'resource_generation_task',
    'next_retry_at',
    'ALTER TABLE resource_generation_task ADD COLUMN next_retry_at timestamp NULL'
);

CALL add_column_if_missing(
    'resource_generation_task',
    'last_error',
    'ALTER TABLE resource_generation_task ADD COLUMN last_error varchar(120)'
);

CALL add_column_if_missing(
    'resource_generation_task',
    'recoverable',
    'ALTER TABLE resource_generation_task ADD COLUMN recoverable boolean NOT NULL DEFAULT false'
);

DROP PROCEDURE add_column_if_missing;
