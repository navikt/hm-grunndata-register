CREATE TABLE IF NOT EXISTS event_item_v1(
    event_id UUID NOT NULL PRIMARY KEY,
    oid UUID NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    event_name VARCHAR(255) NOT NULL,
    by_user VARCHAR(255) NOT NULL,
    extra_key_values JSONB NOT NULL,
    payload JSONB NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX event_item_v1_oid_idx ON event_item_v1(oid);
CREATE INDEX event_item_v1_status_idx ON event_item_v1(status);