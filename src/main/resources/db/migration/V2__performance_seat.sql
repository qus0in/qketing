-- V2: performance · seat 핵심 schema (#28 결정 2A)
CREATE TABLE performance (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title      varchar(200) NOT NULL,
    starts_at  timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE seat (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    performance_id bigint NOT NULL REFERENCES performance (id),
    section        varchar(16) NOT NULL,
    row_label      varchar(8) NOT NULL,
    seat_number    integer NOT NULL CHECK (seat_number > 0),
    created_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_seat_position UNIQUE (performance_id, section, row_label, seat_number)
);

-- seat(performance_id)는 uk_seat_position 선두 컬럼 인덱스로 커버되어 별도 인덱스 없음
