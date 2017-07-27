CREATE TABLE IF NOT EXISTS users (
    user_id UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password TEXT,
    role TEXT
);