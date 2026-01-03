CREATE TABLE IF NOT EXISTS timer (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     timer_name VARCHAR(100) NOT NULL,
                                     start_time INTEGER NOT NULL,
                                     end_time INTEGER NOT NULL,
                                     CONSTRAINT uk_timer_name UNIQUE (timer_name),
                                     CONSTRAINT uk_start_time UNIQUE (start_time),
                                     CONSTRAINT uk_end_time UNIQUE (end_time)
    );

CREATE TABLE IF NOT EXISTS user_timer (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      user_name VARCHAR(30) UNIQUE NOT NULL,
                                        password VARCHAR(255) NOT NULL
    );

-- Indice per velocizzare le query di sovrapposizione
CREATE INDEX IF NOT EXISTS idx_timer_temporal ON timer (start_time, end_time);