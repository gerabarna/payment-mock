DROP TABLE IF EXISTS users, transactions;
DROP SEQUENCE IF EXISTS user_sequence, transaction_sequence;
CREATE SEQUENCE user_sequence START 1 INCREMENT 1;
CREATE TABLE users
(
    id       INTEGER PRIMARY KEY DEFAULT nextval('user_sequence'), -- over 4 billion users does not seem realistic
    balance  DECIMAL(32, 6)      DEFAULT 0,                        -- actual precision may depend here on policy/main balance currency denominations
    currency VARCHAR(3)          DEFAULT 'USD' NOT NULL,
    updated  timestamp NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE transaction_sequence START 1 INCREMENT 1;

CREATE TABLE transactions
(
    id          BIGINT PRIMARY KEY      DEFAULT nextval('transaction_sequence'), -- let's be an optimistic and plan for a long future
    sender_id   INTEGER REFERENCES users (id),
    receiver_id INTEGER REFERENCES users (id),
    request_id  varchar(50),
    amount      DECIMAL(32, 6) NOT NULL,
    currency    VARCHAR(3)     NOT NULL,
    inserted    timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
GRANT ALL PRIVILEGES ON TABLE users, transactions TO kibit;
GRANT ALL PRIVILEGES ON SEQUENCE user_sequence, transaction_sequence TO kibit;

INSERT INTO users(id, balance)
VALUES (1, 100),
       (2, 10000),
       (3, -10),
       (4, 0);