alter table kb_doc_chunk
    add column chunk_hash varchar(64) null;

update kb_doc_chunk
set chunk_hash = lower(sha2(concat_ws('|',
    document_id,
    document_version,
    coalesce(section_title, ''),
    content
), 256))
where chunk_hash is null;

alter table kb_doc_chunk
    modify chunk_hash varchar(64) not null;

create unique index uk_kb_doc_chunk_document_version_hash
    on kb_doc_chunk (document_id, document_version, chunk_hash);

create index idx_kb_doc_chunk_document_version_hash
    on kb_doc_chunk (document_id, document_version, chunk_hash);
