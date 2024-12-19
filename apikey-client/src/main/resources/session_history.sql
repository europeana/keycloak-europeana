CREATE TABLE sessions.session_history (
                                          user_session_id varchar(36) NOT NULL,
                                          us_user_id varchar(255) NOT NULL,
                                          us_realm_id varchar(255) NOT NULL,
                                          us_created_on int4 NOT NULL,
                                          us_ip varchar(36) NULL,
                                          us_last_session_refresh int4 DEFAULT 0 NOT NULL,
                                          us_broker_session_id varchar(1024) NULL,
                                          us_version int4 DEFAULT 0 NULL,
                                          cs_client_id varchar(255) NOT NULL,
                                          cs_timestamp int4 NULL,
                                          cs_started int4 NULL,
                                          cs_user_session_started int4 NULL,
                                          cs_ext_client_id varchar(255) DEFAULT 'local'::character varying NOT NULL,
                                          cs_version int4 DEFAULT 0 NULL,
                                          CONSTRAINT constraint_ses_hist_pk PRIMARY KEY (user_session_id)
);
CREATE INDEX idx_ses_hist_by_broker_session_id ON sessions.session_history USING btree (us_broker_session_id, us_realm_id);
CREATE INDEX idx_ses_hist_by_last_session_refresh ON sessions.session_history USING btree (us_realm_id, us_last_session_refresh);
CREATE INDEX idx_ses_hist_by_ip ON sessions.session_history USING btree (us_realm_id, us_ip);
CREATE INDEX idx_ses_hist_by_user ON sessions.session_history USING btree (us_user_id, us_realm_id);
CREATE INDEX idx_ses_hist_by_client ON sessions.session_history USING btree (cs_client_id, us_realm_id);


-- this trigger fires when a row is inserted in offline_user_session
CREATE TRIGGER after_insert_offline_user_session
    AFTER INSERT
    ON offline_user_session
    FOR EACH ROW
    EXECUTE FUNCTION insert_session_history_row();

-- this code is executed by the above trigger
-- it first pauses for a second to make sure the offline_clients_session row is committed
-- then it looks in the session history if the the last session history row is more than an hour old
-- in which case it adds a new row to the history
-- values from the offline_user_sessions table are passed in the NEW. variables, values from offline_client_sessions
-- were queried separately into the %rowtype parameter. They are combined in the new session_history row


CREATE OR REPLACE FUNCTION insert_session_history_row()
    RETURNS TRIGGER
AS $$
DECLARE
p_row_count int2;
    cs_row offline_client_session%ROWTYPE;
BEGIN
    PERFORM pg_sleep(3);

SELECT COUNT(*) INTO p_row_count
FROM sessions.session_history sh
WHERE to_timestamp(sh.us_last_session_refresh) < (NOW() - interval '1 hour')
  AND sh.user_session_id = NEW.user_session_id;

SELECT * INTO cs_row
FROM offline_client_session cs
WHERE cs.user_session_id = NEW.user_session_id
  AND cs.timestamp = (
    SELECT MAX(cs2.timestamp)
    FROM offline_client_session cs2
    WHERE cs2.user_session_id = cs.user_session_id);

IF p_row_count = 0 THEN
       INSERT INTO sessions.session_history
        ( user_session_id
        , us_user_id
        , us_realm_id
        , us_created_on
        , us_ip
        , us_last_session_refresh
        , us_broker_session_id
        , us_version
        , cs_client_id
        , cs_timestamp
        , cs_started
        , cs_user_session_started
        , cs_ext_client_id
        , cs_version )
      VALUES(
          NEW.user_session_id
        , NEW.user_id
        , NEW.realm_id
        , NEW.created_on
        , NULLIF((REGEXP_MATCH(NEW."data",'ipAddress":"(.+?)"'))[1], '')
        , NEW.last_session_refresh
        , NULLIF(NEW.broker_session_id, '')
        , NULLIF(NEW."version", '')
        , cs_row.client_id
        , NULLIF(cs_row."timestamp", '')
        , NULLIF((REGEXP_MATCH(cs_row."data",'startedAt":"(\d+?)"'))[1], '')::int
        , NULLIF((REGEXP_MATCH(cs_row."data",'userSessionStartedAt":"(\d+?)"'))[1], '')::int
        , cs_row.ext_client_id
        , NULLIF(cs_row."version", ''));
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;



-- below query can be used to see what is available in the KC session tables

SELECT
    cs.user_session_id client_session
     , us.user_session_id user_session
     , c.client_id
     , u.username
     , NULLIF((REGEXP_MATCH(us."data",'ipAddress":"(.+?)"'))[1], '') AS ipaddress
     , to_timestamp(us."created_on") AS created_on
     , to_timestamp(us."last_session_refresh") AS last_refreshed
     , to_timestamp(cs."timestamp") AS clsess_timestamp
     , to_timestamp(NULLIF((REGEXP_MATCH(cs."data",'startedAt":"(\d+?)"'))[1], '')::int) AS clsess_user_session_started_time
     , to_timestamp(NULLIF((REGEXP_MATCH(cs."data",'userSessionStartedAt":"(\d+?)"'))[1], '')::int) AS clsess_user_session_started_time
FROM test_v25.offline_client_session cs
JOIN test_v25.client c ON cs.client_id = c.id
LEFT OUTER JOIN test_v25.offline_user_session us ON cs.user_session_id = us.user_session_id
LEFT OUTER JOIN test_v25.user_entity u ON u.id = us.user_id