ALTER TABLE operators
    ADD COLUMN company_id BIGINT,
    ADD COLUMN operator_type VARCHAR(20);

UPDATE operators SET operator_type = 'SYSTEM' WHERE operator_type IS NULL;

ALTER TABLE operators
    ALTER COLUMN operator_type SET NOT NULL;

ALTER TABLE operators
    ADD CONSTRAINT fk_operator_company
        FOREIGN KEY (company_id) REFERENCES companies(id);

CREATE INDEX idx_operators_company_id ON operators(company_id);
CREATE INDEX idx_operators_operator_type ON operators(operator_type);
