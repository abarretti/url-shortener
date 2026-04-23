CREATE INDEX idx_urls_user_id_created_at_desc
    ON urls(user_id, created_at DESC);
