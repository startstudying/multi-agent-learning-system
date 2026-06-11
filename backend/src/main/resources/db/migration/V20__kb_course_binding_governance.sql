alter table kb_knowledge_base
    add column course_id varchar(80) null,
    add column binding_status varchar(40) not null default 'UNBOUND',
    add column bound_by varchar(120) null,
    add column bound_at datetime(6) null;

update kb_knowledge_base kb
join (
    select
        document.kb_id,
        count(*) as document_count,
        sum(case when nullif(trim(document.course_id), '') is null then 1 else 0 end) as null_course_count,
        count(distinct nullif(trim(document.course_id), '')) as course_count,
        min(nullif(trim(document.course_id), '')) as single_course_id,
        sum(case
            when nullif(trim(document.course_id), '') is not null and course.id is null then 1
            else 0
        end) as invalid_course_count
    from kb_document document
    left join course course on course.id = nullif(trim(document.course_id), '')
    where document.deleted_at is null
    group by document.kb_id
) document_scope on document_scope.kb_id = kb.id
set kb.course_id = document_scope.single_course_id,
    kb.binding_status = 'BOUND',
    kb.bound_by = kb.created_by,
    kb.bound_at = coalesce(kb.updated_at, kb.created_at, now(6))
where kb.deleted_at is null
  and document_scope.document_count > 0
  and document_scope.null_course_count = 0
  and document_scope.course_count = 1
  and document_scope.invalid_course_count = 0;

update kb_knowledge_base kb
join (
    select
        document.kb_id,
        count(*) as document_count,
        sum(case when nullif(trim(document.course_id), '') is null then 1 else 0 end) as null_course_count,
        count(distinct nullif(trim(document.course_id), '')) as course_count,
        sum(case
            when nullif(trim(document.course_id), '') is not null and course.id is null then 1
            else 0
        end) as invalid_course_count
    from kb_document document
    left join course course on course.id = nullif(trim(document.course_id), '')
    where document.deleted_at is null
    group by document.kb_id
) document_scope on document_scope.kb_id = kb.id
set kb.course_id = null,
    kb.binding_status = 'CONFLICTED',
    kb.bound_by = null,
    kb.bound_at = null
where kb.deleted_at is null
  and document_scope.document_count > 0
  and document_scope.null_course_count <> document_scope.document_count
  and not (
      document_scope.null_course_count = 0
      and document_scope.course_count = 1
      and document_scope.invalid_course_count = 0
  );

create index idx_kb_course_binding
    on kb_knowledge_base (course_id, binding_status, deleted_at);

create index idx_kb_document_kb_course_deleted
    on kb_document (kb_id, course_id, deleted_at);

alter table kb_knowledge_base
    add constraint ck_kb_binding_status
        check (binding_status in ('UNBOUND', 'BOUND', 'CONFLICTED')),
    add constraint ck_kb_binding_course_consistency
        check (binding_status <> 'BOUND' or course_id is not null),
    add constraint fk_kb_course_binding_course
        foreign key (course_id) references course(id);

