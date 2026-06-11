create table if not exists model_provider (
    id varchar(80) not null,
    provider_code varchar(80) not null,
    display_name varchar(120) not null,
    remark varchar(500) null,
    website_url varchar(500) null,
    base_url varchar(500) not null,
    chat_model varchar(120) null,
    embedding_model varchar(120) null,
    api_key_ciphertext varchar(1024) not null,
    enabled tinyint(1) not null default 1,
    is_default tinyint(1) not null default 0,
    created_by varchar(120) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    unique key uk_model_provider_code (provider_code),
    key idx_model_provider_default_enabled (is_default, enabled)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
