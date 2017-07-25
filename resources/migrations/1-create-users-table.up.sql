CREATE TABLE IF NOT EXISTS users (
    user_id UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    hash TEXT,
    role TEXT
);