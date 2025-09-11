create table sykmelding(
    sykmeldingId varchar PRIMARY KEY not null,
    ident varchar not null,
    sykmelding jsonb not null
);

create index sykmelding_ident_idx on sykmelding(ident);
