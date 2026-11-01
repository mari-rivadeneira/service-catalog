DROP TABLE IF EXISTS weekly_schedules;
DROP TABLE IF EXISTS professionals;
DROP TABLE IF EXISTS professional_categories;
DROP TABLE IF EXISTS sync_state;

CREATE TABLE professional_categories (
                                         id BIGINT PRIMARY KEY,
                                         name VARCHAR(100) NOT NULL,
                                         description TEXT,
                                         enabled BOOLEAN NOT NULL,
                                         created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                         updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE professionals (
                               id BIGINT PRIMARY KEY,
                               category_id BIGINT NOT NULL,
                               first_name VARCHAR(100) NOT NULL,
                               last_name VARCHAR(100) NOT NULL,
                               enabled BOOLEAN NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               CONSTRAINT fk_professionals_category FOREIGN KEY (category_id)
                                   REFERENCES professional_categories(id) ON DELETE RESTRICT
);

CREATE TABLE weekly_schedules (
                                  id BIGINT PRIMARY KEY,
                                  professional_id BIGINT NOT NULL,
                                  day_of_week VARCHAR(15) NOT NULL,
                                  start_time TIME NOT NULL,
                                  end_time TIME NOT NULL,
                                  slot_duration_minutes INT NOT NULL,
                                  enabled BOOLEAN NOT NULL,
                                  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  CONSTRAINT fk_weekly_schedules_professional FOREIGN KEY (professional_id)
                                      REFERENCES professionals(id) ON DELETE CASCADE,
                                  CONSTRAINT chk_time_window CHECK (start_time < end_time),
                                  CONSTRAINT chk_positive_duration CHECK (slot_duration_minutes > 0),
                                  CONSTRAINT chk_valid_day_of_week CHECK (day_of_week IN
                                                                          ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))
);

CREATE TABLE sync_state (
                            entity_name VARCHAR(100) PRIMARY KEY,
                            version BIGINT NOT NULL,
                            last_synced_at TIMESTAMP WITH TIME ZONE NOT NULL,
                            last_mode VARCHAR(20),
                            last_error TEXT
);

CREATE INDEX idx_professionals_category ON professionals(category_id);
CREATE INDEX idx_weekly_schedules_professional ON weekly_schedules(professional_id);