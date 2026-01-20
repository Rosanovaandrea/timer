CREATE TABLE IF NOT EXISTS timer (
id INTEGER PRIMARY KEY,
timer_name VARCHAR(100) NOT NULL,
start_time INTEGER NOT NULL,
end_time INTEGER NOT NULL,
CONSTRAINT uk_start_time UNIQUE (start_time),
CONSTRAINT uk_end_time UNIQUE (end_time)
);

CREATE TABLE IF NOT EXISTS user_timer (
id INTEGER PRIMARY KEY,
user_name VARCHAR(30) UNIQUE NOT NULL,
password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS monitor_timer (
id INTEGER PRIMARY KEY,
start INT NOT NULL UNIQUE,
stop INT NOT NULL
);

-- Indice per velocizzare le query di sovrapposizione
CREATE INDEX IF NOT EXISTS idx_timer_temporal ON timer (start_time, end_time);