--CREATE TABLE IF NOT EXISTS users (
--    id UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL PRIMARY KEY,
--    username TEXT UNIQUE NOT NULL,
--    password TEXT,
--    role TEXT
--);

CREATE TABLE mymine (
  user_id integer NOT NULL,
  mine text NOT NULL,
  data jsonb,
  PRIMARY KEY (user_id, mine)
);