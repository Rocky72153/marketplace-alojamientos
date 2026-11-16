-- Run ONLY on a new, empty disposable PostgreSQL database with ON_ERROR_STOP=1.
CREATE TABLE public.reservations (
    id integer PRIMARY KEY,
    status varchar(24) NOT NULL,
    CONSTRAINT reservations_status_check CHECK
        (status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED'))
);
INSERT INTO public.reservations VALUES (1, 'CONFIRMED'), (2, 'CANCELLED'), (3, 'COMPLETED');

\ir 20261114-reservation-pending-payment.sql
\ir 20261114-reservation-pending-payment.sql

INSERT INTO public.reservations VALUES (4, 'PENDING_PAYMENT');
DO $$
BEGIN
    IF (SELECT count(*) FROM public.reservations) <> 4 THEN
        RAISE EXCEPTION 'Existing reservations were not preserved';
    END IF;
    BEGIN
        INSERT INTO public.reservations VALUES (5, 'UNKNOWN');
        RAISE EXCEPTION 'Unknown status must not be accepted';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;
END $$;
