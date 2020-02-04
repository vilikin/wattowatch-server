CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE channels (
    id SERIAL PRIMARY KEY,
    source_system VARCHAR NOT NULL,
    id_in_source_system VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    url VARCHAR
);

CREATE TABLE videos (
    id BIGSERIAL NOT NULL,
    id_in_source_system VARCHAR NOT NULL,
    channel_id INT NOT NULL REFERENCES channels (id),
    title VARCHAR NOT NULL,
    url VARCHAR,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    persisted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE live_streams (
    id BIGSERIAL NOT NULL,
    id_in_source_system VARCHAR NOT NULL,
    channel_id INT NOT NULL REFERENCES channels (id),
    title VARCHAR NOT NULL,
    live BOOLEAN NOT NULL,
    live_since TIMESTAMP WITH TIME ZONE
);

CREATE TABLE user_is_subscribed_to_channel (
    user_id INT NOT NULL REFERENCES users (id),
    channel_id INT NOT NULL REFERENCES channels (id)
);
