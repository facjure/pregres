CREATE TABLE users (
  id serial PRIMARY KEY,
  created_at TIMESTAMP NOT NULL DEFAULT (now() at time zone 'utc'),
  username VARCHAR(64) UNIQUE,
  email VARCHAR(64) UNIQUE,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  phone_number VARCHAR(32) UNIQUE,
  details jsonb
);
