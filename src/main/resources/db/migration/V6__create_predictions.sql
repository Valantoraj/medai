-- V6: Create predictions table for ML prediction history
CREATE TABLE IF NOT EXISTS predictions (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT REFERENCES users(id) ON DELETE CASCADE,
    prediction_type     VARCHAR(10) NOT NULL,
    disease             VARCHAR(100) NOT NULL,
    risk_probability    DECIMAL(5,2),
    risk_level          VARCHAR(10),
    predicted_class     VARCHAR(100),
    confidence_pct      DECIMAL(5,2),
    guidance_text       TEXT,
    model_used          VARCHAR(200),
    created_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_predictions_user_id ON predictions(user_id);
CREATE INDEX idx_predictions_disease ON predictions(disease);
CREATE INDEX idx_predictions_created_at ON predictions(created_at);
