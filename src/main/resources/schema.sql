DO $$
BEGIN
    ALTER TABLE licenses
        ADD CONSTRAINT fk_license_product FOREIGN KEY (product_id) REFERENCES products (id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$^^^

DO $$
BEGIN
    ALTER TABLE licenses
        ADD CONSTRAINT fk_license_type FOREIGN KEY (type_id) REFERENCES license_types (id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$^^^

DO $$
BEGIN
    ALTER TABLE device_licenses
        ADD CONSTRAINT fk_device_license_license FOREIGN KEY (license_id) REFERENCES licenses (id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$^^^

DO $$
BEGIN
    ALTER TABLE device_licenses
        ADD CONSTRAINT fk_device_license_device FOREIGN KEY (device_id) REFERENCES devices (id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$^^^

DO $$
BEGIN
    ALTER TABLE license_history
        ADD CONSTRAINT fk_license_history_license FOREIGN KEY (license_id) REFERENCES licenses (id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$^^^

DO $$
BEGIN
    ALTER TABLE signatures_history
        ADD CONSTRAINT fk_signature_history_signature FOREIGN KEY (signature_id) REFERENCES signatures (id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$^^^

DO $$
BEGIN
    ALTER TABLE signatures_audit
        ADD CONSTRAINT fk_signature_audit_signature FOREIGN KEY (signature_id) REFERENCES signatures (id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$^^^
