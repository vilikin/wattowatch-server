ALTER TABLE videos
ADD CONSTRAINT videos_unique UNIQUE (id_in_source_system, channel_id);
