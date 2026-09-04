CREATE TABLE iso_v22
(
    id               uuid                    NOT NULL PRIMARY KEY,
    iso_code         character varying(32)   NOT NULL UNIQUE,
    iso_title        character varying(1024) NOT NULL,
    iso_text         text,
    iso_type         character varying(32)   NOT NULL,
    iso_translations jsonb                   NOT NULL,
    created_by_user  character varying(255)  NOT NULL,
    updated_by_user  character varying(255)  NOT NULL,
    created_by       character varying(32)   NOT NULL,
    updated_by       character varying(32)   NOT NULL,
    created          timestamp without time zone NOT NULL,
    updated          timestamp without time zone NOT NULL,
    search_words     jsonb DEFAULT '[]'::jsonb NOT NULL
);

CREATE TABLE iso_map_v22
(
    id      uuid        NOT NULL PRIMARY KEY,
    code16  VARCHAR(32),
    code22  VARCHAR(32),
    map_enum jsonb DEFAULT '[]'::jsonb NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    verified  BOOLEAN NOT NULL
);

create unique index idx_iso_map_v22_code16_code22 on iso_map_v22 (code16, code22);