CREATE TABLE news_reg_v1 (
    id UUID PRIMARY KEY,
    title VARCHAR(512) NOT NULL,
    text TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    draft_status VARCHAR(32) NOT NULL,
    published TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    author VARCHAR(255) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    created_by_user VARCHAR(255) NOT NULL,
    updated_by_user VARCHAR(255) NOT NULL
);

CREATE INDEX news_reg_v1_status_idx ON news_reg_v1 (status);
CREATE INDEX news_reg_v1_updated_idx ON news_reg_v1 (updated);
CREATE INDEX news_reg_v1_draft_status_idx ON news_reg_v1 (draft_status);