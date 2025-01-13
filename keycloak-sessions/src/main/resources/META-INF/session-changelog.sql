-- liquibase formatted sql

-- changeset luthien:1 labels: create_table context: add_session_history
CREATE TABLE session_history (
    id varchar(36) NOT NULL,
    user_id varchar(36) NULL,
    client_id varchar(36) NULL,
    ip_address varchar(255) NULL,
    realm_id varchar(255) NULL,
    created_timestamp int8 NULL,
    updated_timestamp int8 NULL,
    session_id varchar(36) NULL,
    CONSTRAINT session_history_pk PRIMARY KEY (id)
);
--rollback DROP TABLE session_history;

-- changeset luthien:2 labels: create_indexes context: add_session_history
CREATE INDEX idx_session_history_user ON session_history USING btree (user_id);
CREATE INDEX idx_session_history_client ON session_history USING btree (client_id);
CREATE INDEX idx_session_history_ip ON session_history USING btree (ip_address);
CREATE INDEX idx_session_history ON session_history USING btree (user_id, client_id, ip_address);
-- rollback DROP INDEX idx_session_history_user, idx_session_history_client, idx_session_history_ip, idx_session_history;