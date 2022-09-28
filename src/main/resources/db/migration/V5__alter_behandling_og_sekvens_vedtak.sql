ALTER TABLE behandling
    DROP COLUMN skal_behandles_automatisk;

ALTER TABLE behandling
    ADD COLUMN soknad_mottatt_dato TIMESTAMP(3);

CREATE SEQUENCE behandling_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE vedtak_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;