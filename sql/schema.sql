CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    role VARCHAR(20) NOT NULL,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE UNIQUE INDEX uk_username ON sys_user(username);
CREATE INDEX idx_role ON sys_user(role);

CREATE TABLE IF NOT EXISTS pipeline (
    pipeline_id VARCHAR(64) PRIMARY KEY,
    pipeline_name VARCHAR(100),
    geo_coordinates GEOMETRY(LineString, 4326) NOT NULL,
    material_type VARCHAR(50),
    diameter NUMERIC(8,2),
    install_time DATE,
    region_code VARCHAR(20),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_pipeline_geom ON pipeline USING GIST(geo_coordinates);
CREATE INDEX idx_material ON pipeline(material_type);
CREATE INDEX idx_install_time ON pipeline(install_time);

CREATE TABLE IF NOT EXISTS inspection_record (
    record_id BIGSERIAL PRIMARY KEY,
    pipeline_id VARCHAR(64) NOT NULL,
    user_id BIGINT,
    image_path VARCHAR(500) NOT NULL,
    image_name VARCHAR(200),
    detection_result VARCHAR(20),
    confidence_score NUMERIC(5,4),
    inspect_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_record_pipeline ON inspection_record(pipeline_id);
CREATE INDEX idx_inspect_time ON inspection_record(inspect_time);
CREATE INDEX idx_detection_result ON inspection_record(detection_result);

CREATE TABLE IF NOT EXISTS detection_task (
    task_id BIGSERIAL PRIMARY KEY,
    record_id BIGINT UNIQUE,
    status VARCHAR(20) NOT NULL,
    retry_count SMALLINT DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_task_status ON detection_task(status);
CREATE INDEX idx_task_created ON detection_task(created_at);

CREATE TABLE IF NOT EXISTS defect (
    defect_id BIGSERIAL PRIMARY KEY,
    record_id BIGINT,
    pipeline_id VARCHAR(64) NOT NULL,
    defect_type VARCHAR(20) NOT NULL,
    severity_level SMALLINT NOT NULL,
    location GEOMETRY(Point, 4326),
    bbox VARCHAR(100),
    confidence_score NUMERIC(5,4),
    source VARCHAR(20),
    detected_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_defect_pipeline ON defect(pipeline_id);
CREATE INDEX idx_defect_type ON defect(defect_type);
CREATE INDEX idx_severity ON defect(severity_level);
CREATE INDEX idx_defect_geom ON defect USING GIST(location);
CREATE INDEX idx_detected_at ON defect(detected_at);

CREATE TABLE IF NOT EXISTS alert_record (
    alert_id BIGSERIAL PRIMARY KEY,
    defect_id BIGINT,
    pipeline_id VARCHAR(64),
    alert_level SMALLINT NOT NULL,
    alert_type VARCHAR(30) NOT NULL,
    alert_message VARCHAR(500),
    is_read BOOLEAN DEFAULT FALSE,
    triggered_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_pipeline ON alert_record(pipeline_id);
CREATE INDEX idx_alert_read ON alert_record(is_read);
CREATE INDEX idx_triggered_at ON alert_record(triggered_at);

CREATE TABLE IF NOT EXISTS risk_report (
    report_id BIGSERIAL PRIMARY KEY,
    report_title VARCHAR(200) NOT NULL,
    region_code VARCHAR(20),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    total_defects INT,
    high_risk_count INT,
    report_content JSONB,
    file_path VARCHAR(500),
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_report_region ON risk_report(region_code);
CREATE INDEX idx_report_time ON risk_report(start_time, end_time);

CREATE TABLE IF NOT EXISTS operation_log (
    log_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    module VARCHAR(50) NOT NULL,
    operation VARCHAR(100) NOT NULL,
    request_uri VARCHAR(200),
    request_params TEXT,
    result VARCHAR(20),
    error_msg TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_log_user ON operation_log(user_id);
CREATE INDEX idx_log_module ON operation_log(module);
CREATE INDEX idx_log_created ON operation_log(created_at);

ALTER TABLE inspection_record ADD CONSTRAINT fk_record_pipeline FOREIGN KEY (pipeline_id) REFERENCES pipeline(pipeline_id);
ALTER TABLE inspection_record ADD CONSTRAINT fk_record_user FOREIGN KEY (user_id) REFERENCES sys_user(id);
ALTER TABLE detection_task ADD CONSTRAINT fk_task_record FOREIGN KEY (record_id) REFERENCES inspection_record(record_id);
ALTER TABLE defect ADD CONSTRAINT fk_defect_record FOREIGN KEY (record_id) REFERENCES inspection_record(record_id);
ALTER TABLE defect ADD CONSTRAINT fk_defect_pipeline FOREIGN KEY (pipeline_id) REFERENCES pipeline(pipeline_id);
ALTER TABLE alert_record ADD CONSTRAINT fk_alert_defect FOREIGN KEY (defect_id) REFERENCES defect(defect_id);
ALTER TABLE alert_record ADD CONSTRAINT fk_alert_pipeline FOREIGN KEY (pipeline_id) REFERENCES pipeline(pipeline_id);
ALTER TABLE risk_report ADD CONSTRAINT fk_report_user FOREIGN KEY (created_by) REFERENCES sys_user(id);
ALTER TABLE operation_log ADD CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id);
