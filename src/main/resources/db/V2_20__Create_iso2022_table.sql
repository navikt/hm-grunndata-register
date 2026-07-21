CREATE TABLE iso_v22
(
    id               uuid                    NOT NULL PRIMARY KEY,
    iso_code         character varying(32)   NOT NULL UNIQUE,
    iso_title        character varying(1024) NOT NULL,
    iso_text         text,
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
    id      uuid        NOT NULL,
    code16  VARCHAR(32) NOT NULL,
    code22  VARCHAR(32) NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL
);