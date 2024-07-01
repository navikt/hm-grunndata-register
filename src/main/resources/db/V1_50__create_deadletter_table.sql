CREATE TABLE IF NOT EXISTS hm_dead_letter_v1
(
    event_id    VARCHAR(255) PRIMARY KEY,
    event_name  VARCHAR(255) NOT NULL,
    json        TEXT         NOT NULL,
    error       TEXT         NOT NULL,
    created     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    topic       VARCHAR(255) NOT NULL,
    river_name  VARCHAR(255) NOT NULL
);