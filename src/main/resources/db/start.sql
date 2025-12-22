CREATE TABLE IF NOT EXISTS timer (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     timer_name TEXT UNIQUE NOT NULL,
                                     start_time UNIQUE INTEGER NOT NULL,
                                     end_time UNIQUE INTEGER NOT NULL
    );

-- Indice per velocizzare le query di sovrapposizione e ordinamento
CREATE INDEX IF NOT EXISTS idx_timer_temporal ON timer (start_time, end_time);