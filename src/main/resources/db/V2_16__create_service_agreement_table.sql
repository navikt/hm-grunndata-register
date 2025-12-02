CREATE TABLE service_agreement_v1
(
    id              UUID         NOT NULL PRIMARY KEY,
    service_id      UUID         NOT NULL,
    agreement_id    UUID         NOT NULL,
    supplier_id     UUID         NOT NULL,
    supplier_ref    VARCHAR(255) NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    created_by      VARCHAR(64)  NOT NULL,
    updated_by      VARCHAR(64)  NOT NULL,
    updated_by_user VARCHAR(255) NOT NULL,
    created         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX service_agreement_v1_idx_service_id
    ON service_agreement_v1 (service_id);
CREATE INDEX service_agreement_v1_idx_agreement_id
    ON service_agreement_v1 (agreement_id);
CREATE UNIQUE INDEX service_agreement_v1_idx_unique_service_id_agreement_id
    ON service_agreement_v1 (service_id, agreement_id);