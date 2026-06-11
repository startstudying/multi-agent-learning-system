alter table agent_tool_call
    add column trace_id varchar(120) null,
    add column input_summary varchar(2000) null,
    add column output_summary varchar(2000) null,
    add column retention_class varchar(80) null;

create index idx_agent_tool_call_trace on agent_tool_call (trace_id);
create index idx_agent_tool_call_status on agent_tool_call (status);
