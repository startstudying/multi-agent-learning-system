alter table resource_review
    add column reason varchar(2000) null,
    add column citation_check varchar(2000) null,
    add column safety_check varchar(2000) null,
    add column revision_suggestion varchar(2000) null;
