-- 1. Tabla de categorias profesionales
CREATE TABLE IF NOT EXISTS professional_categories (
                                                       id UUID PRIMARY KEY,
                                                       name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
    );

-- 2. Tabla de profesionales
CREATE TABLE IF NOT EXISTS professionals (
                                             id UUID PRIMARY KEY,
                                             first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(50),
    category_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_professionals_category FOREIGN KEY (category_id) REFERENCES professional_categories(id) ON DELETE RESTRICT
    );

-- 3. Tabla de agendas semanales
CREATE TABLE IF NOT EXISTS weekly_schedules (
                                                id UUID PRIMARY KEY,
                                                professional_id UUID NOT NULL,
                                                day_of_week VARCHAR(15) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INT NOT NULL,
    CONSTRAINT fk_weekly_schedules_professional FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE,
    CONSTRAINT chk_time_window CHECK (start_time < end_time),
    CONSTRAINT chk_positive_duration CHECK (slot_duration_minutes > 0),
    CONSTRAINT chk_valid_day_of_week CHECK (
                                               day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')
    )
    );

-- 4. Tabla de control de sincronizacion
CREATE TABLE IF NOT EXISTS sync_state (
                                          entity_name VARCHAR(100) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 1,
    last_synced_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
    );

-- Indices auxiliares para consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_professionals_category ON professionals(category_id);
CREATE INDEX IF NOT EXISTS idx_weekly_schedules_professional ON weekly_schedules(professional_id);