CREATE TABLE iso_map(
    id              uuid          NOT NULL,
    code16          VARCHAR(32)   NOT NULL,
    code22          VARCHAR(32)   NOT NULL,
    created         TIMESTAMP WITHOUT TIME ZONE NOT NULL
);