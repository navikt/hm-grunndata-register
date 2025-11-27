CREATE TABLE service_offering_v1
(
    id           UUID                     NOT NULL PRIMARY KEY,
    title        TEXT                     NOT NULL,
    supplier_ref VARCHAR(255)             NOT NULL,
    hms_art_nr   VARCHAR(255)             NOT NULL,
    supplier_id  UUID                     NOT NULL,
    iso_category VARCHAR(32)              NOT NULL,
    published    TIMESTAMP                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired      TIMESTAMP                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created      TIMESTAMP                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    draft_status VARCHAR(32)              NOT NULL,
    status       VARCHAR(32)              NOT NULL,
    service_data JSONB                    NOT NULL,
    version      BIGINT                   NOT NULL
);

CREATE UNIQUE INDEX unique_service_offering_v1_supplier_id_hms_art_nr
    ON service_task_v1 (supplier_id, hms_art_nr);