#!/bin/bash

until nc -z postgres 5432; do
  >&2 echo "Postgres is unavailable - retrying in 1s"
  sleep 1
done

>&2 echo "Postgres is up, continuing"
exec $cmd
