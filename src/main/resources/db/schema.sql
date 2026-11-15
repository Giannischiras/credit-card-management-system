-- VCT Credit Card Company schema (MySQL 8+)
-- The available credit of an account is not stored: it is always credit_limit - debt.

CREATE TABLE IF NOT EXISTS card_account (
    account_number  INT            PRIMARY KEY,
    name            VARCHAR(255)   NOT NULL,
    account_type    VARCHAR(16)    NOT NULL,
    expiration_date CHAR(7)        NOT NULL,
    credit_limit    DECIMAL(14, 2) NOT NULL,
    debt            DECIMAL(14, 2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_card_account_name UNIQUE (account_type, name),
    CONSTRAINT chk_card_account_type CHECK (account_type IN ('INDIVIDUAL', 'COMPANY')),
    CONSTRAINT chk_card_account_limit CHECK (credit_limit > 0)
);

CREATE TABLE IF NOT EXISTS merchant (
    account_number  INT            PRIMARY KEY,
    name            VARCHAR(255)   NOT NULL,
    commission_rate DECIMAL(5, 4)  NOT NULL,
    total_profit    DECIMAL(14, 2) NOT NULL DEFAULT 0,
    debt            DECIMAL(14, 2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_merchant_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS employee (
    employee_id     INT          PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    company_account INT          NOT NULL,
    CONSTRAINT fk_employee_company FOREIGN KEY (company_account)
        REFERENCES card_account (account_number) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS card_transaction (
    id               BIGINT         AUTO_INCREMENT PRIMARY KEY,
    account_number   INT            NOT NULL,
    merchant_account INT            NOT NULL,
    employee_id      INT            NULL,
    transaction_date DATE           NOT NULL,
    amount           DECIMAL(14, 2) NOT NULL,
    transaction_type VARCHAR(16)    NOT NULL,
    CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('PURCHASE', 'REFUND')),
    CONSTRAINT chk_transaction_amount CHECK (amount > 0),
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_number)
        REFERENCES card_account (account_number) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_merchant FOREIGN KEY (merchant_account)
        REFERENCES merchant (account_number) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_employee FOREIGN KEY (employee_id)
        REFERENCES employee (employee_id) ON DELETE SET NULL
);
