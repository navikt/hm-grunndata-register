CREATE TABLE iso_v22
(
    id               uuid                    NOT NULL,
    iso_code         character varying(32)   NOT NULL,
    iso_title        character varying(1024) NOT NULL,
    iso_title_short  text,
    iso_text         text                    NOT NULL,
    iso_translations jsonb                   NOT NULL,
    created_by_user  character varying(255)  NOT NULL,
    updated_by_user  character varying(255)  NOT NULL,
    created_by       character varying(32)   NOT NULL,
    updated_by       character varying(32)   NOT NULL,
    created          timestamp without time zone NOT NULL,
    updated          timestamp without time zone NOT NULL,
    search_words     jsonb DEFAULT '[]'::jsonb NOT NULL
);