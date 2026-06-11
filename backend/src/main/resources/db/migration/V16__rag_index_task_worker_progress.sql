alter table kb_index_task
    add column progress_percent int not null default 0,
    add column progress_phase varchar(80) not null default 'PENDING',
    add column heartbeat_at datetime(6) null,
    add column lease_owner varchar(120) null,
    add column lease_until datetime(6) null,
    add column next_retry_at datetime(6) null,
    add column recoverable boolean not null default true;

create index idx_kb_index_task_due on kb_index_task (status, next_retry_at, created_at);
create index idx_kb_index_task_lease on kb_index_task (status, lease_until);
