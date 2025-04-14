CREATE SEQUENCE user_sequence MINVALUE 1 INCREMENT BY 1;
CREATE TABLE users(
                      id          INTEGER             DEFAULT nextval('user_sequence') PRIMARY KEY,        -- over 4 billion users does not seem realistic
                      balance     DECIMAL(32, 6)      DEFAULT 0,    -- actual precision may depend here on policy/main balance currency denominations
                      currency    VARCHAR(3)          DEFAULT 'USD'   NOT NULL
);

CREATE SEQUENCE transaction_sequence MINVALUE 1 INCREMENT BY 1;
CREATE TABLE transactions(
                             id          BIGINT          DEFAULT nextval('transaction_sequence') PRIMARY KEY, -- let's be an optimistic and plan for a long future
                             user_id     INTEGER         REFERENCES  users (id),
                             request_id  varchar(50),
                             inserted    timestamp       NOT NULL    DEFAULT CURRENT_TIMESTAMP,
                             amount      DECIMAL(32, 6)  NOT NULL,
                             currency    VARCHAR(3)      NOT NULL
);
