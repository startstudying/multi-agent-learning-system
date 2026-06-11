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
    'model_call_log',
    'prompt_code',
    'ALTER TABLE model_call_log ADD COLUMN prompt_code varchar(120)'
);

CALL add_column_if_missing(
    'model_call_log',
    'prompt_version',
    'ALTER TABLE model_call_log ADD COLUMN prompt_version varchar(120)'
);

CALL add_column_if_missing(
    'model_call_log',
    'temperature',
    'ALTER TABLE model_call_log ADD COLUMN temperature double'
);

CALL add_column_if_missing(
    'model_call_log',
    'structured_output_schema',
    'ALTER TABLE model_call_log ADD COLUMN structured_output_schema varchar(1000)'
);

DROP PROCEDURE add_column_if_missing;
