CREATE TABLE IF NOT EXISTS series_reg_version_v1
(
    version_id          uuid        NOT NULL PRIMARY KEY,
    series_id           uuid        NOT NULL,
    status              VARCHAR(32) NOT NULL,
    admin_status        VARCHAR(32) NOT NULL,
    draft_status        VARCHAR(32) NOT NULL,
    updated             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    series_registration jsonb       NOT NULL,
    version             BIGINT      NOT NULL
);

CREATE INDEX IF NOT EXISTS series_reg_version_v1_series_admin_status_draft_status_id_idx ON series_reg_version_v1 (series_id, admin_status, draft_status);
CREATE INDEX IF NOT EXISTS series_reg_version_v1_version_idx ON series_reg_version_v1 (updated);

