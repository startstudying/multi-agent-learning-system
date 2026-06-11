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
    'kb_query_log',
    'request_id',
    'ALTER TABLE kb_query_log ADD COLUMN request_id varchar(120)'
);

CALL add_column_if_missing(
    'kb_query_log',
    'request_hash',
    'ALTER TABLE kb_query_log ADD COLUMN request_hash varchar(128)'
);

CALL add_column_if_missing(
    'kb_query_log',
    'response_json',
    'ALTER TABLE kb_query_log ADD COLUMN response_json text'
);

CALL add_index_if_missing(
    'kb_query_log',
    'uk_kb_query_user_request',
    'CREATE UNIQUE INDEX uk_kb_query_user_request ON kb_query_log(user_id, request_id)'
);

DROP PROCEDURE add_index_if_missing;
DROP PROCEDURE add_column_if_missing;
