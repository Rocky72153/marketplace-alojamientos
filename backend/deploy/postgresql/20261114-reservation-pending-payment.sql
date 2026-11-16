-- Upgrade only the known reservation status constraint. Run with ON_ERROR_STOP=1,
-- while the application is stopped and after a verified database backup.
-- No reservation rows are deleted or changed. PostgreSQL DDL is transactional.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';
LOCK TABLE public.reservations IN ACCESS EXCLUSIVE MODE;

DO $$
DECLARE
    constraint_sql text;
    allowed text[];
    status_attnum smallint;
    constraint_columns smallint[];
BEGIN
    SELECT attnum INTO status_attnum
    FROM pg_attribute
    WHERE attrelid = 'public.reservations'::regclass
      AND attname = 'status' AND NOT attisdropped;

    SELECT pg_get_constraintdef(oid), conkey
    INTO constraint_sql, constraint_columns
    FROM pg_constraint
    WHERE conrelid = 'public.reservations'::regclass
      AND conname = 'reservations_status_check'
      AND contype = 'c' AND convalidated;

    IF constraint_sql IS NULL OR status_attnum IS NULL
        OR constraint_columns IS DISTINCT FROM ARRAY[status_attnum]::smallint[] THEN
        RAISE EXCEPTION 'Reservation status constraint is missing or unexpected; review schema manually';
    END IF;

    SELECT array_agg(DISTINCT m[1] ORDER BY m[1]) INTO allowed
    FROM regexp_matches(constraint_sql, '''([A-Z_]+)''', 'g') AS m;

    IF allowed = ARRAY['CANCELLED', 'COMPLETED', 'CONFIRMED', 'PENDING_PAYMENT']::text[] THEN
        RETURN; -- already migrated
    END IF;
    IF allowed IS DISTINCT FROM ARRAY['CANCELLED', 'COMPLETED', 'CONFIRMED']::text[] THEN
        RAISE EXCEPTION 'Reservation status constraint has unknown values; review schema manually';
    END IF;

    ALTER TABLE public.reservations DROP CONSTRAINT reservations_status_check;
    ALTER TABLE public.reservations ADD CONSTRAINT reservations_status_check
        CHECK (status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED', 'PENDING_PAYMENT'));
END $$;
COMMIT;
