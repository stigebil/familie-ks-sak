CREATE TABLE FEILUTBETALT_VALUTA
(
    ID                  BIGINT PRIMARY KEY,
    FK_BEHANDLING_ID    BIGINT REFERENCES BEHANDLING (ID)   NOT NULL,
    FOM                 TIMESTAMP(3) NOT NULL ,
    TOM                 TIMESTAMP(3) NOT NULL ,
    FEILUTBETALT_BELOEP NUMERIC,

    -- Base entitet felter
    VERSJON          BIGINT       DEFAULT 0              NOT NULL,
    OPPRETTET_AV     VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID    TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    ENDRET_AV        VARCHAR,
    ENDRET_TID       TIMESTAMP(3)
);

CREATE SEQUENCE FEILUTBETALT_VALUTA_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX FEILUTBETALT_VALUTA_FK_BEHANDLING_ID__IDX ON FEILUTBETALT_VALUTA (FK_BEHANDLING_ID);