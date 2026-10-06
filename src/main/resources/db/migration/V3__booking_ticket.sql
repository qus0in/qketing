-- V3: booking · ticket · seat sale_status (#33, T33-02 설계 C, T33-03 L1 반영)
-- L1: ticket→seat composite FK(fk_ticket_seat)는 booking composite FK와 중복이라 제거.
-- uk_seat_performance_id · uk_booking_reference는 composite FK 참조 대상이므로 유지한다.

ALTER TABLE seat
    ADD COLUMN sale_status varchar(16) NOT NULL DEFAULT 'AVAILABLE',
    ADD CONSTRAINT ck_seat_sale_status
        CHECK (sale_status IN ('AVAILABLE', 'SOLD')),
    ADD CONSTRAINT uk_seat_performance_id UNIQUE (performance_id, id);

CREATE TABLE booking (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idempotency_key varchar(128) NOT NULL,
    performance_id bigint NOT NULL,
    seat_id bigint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_booking_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT uk_booking_reference UNIQUE (id, performance_id, seat_id),
    CONSTRAINT fk_booking_seat FOREIGN KEY (performance_id, seat_id)
        REFERENCES seat (performance_id, id)
);

CREATE TABLE ticket (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id bigint NOT NULL,
    performance_id bigint NOT NULL,
    seat_id bigint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_ticket_booking UNIQUE (booking_id),
    CONSTRAINT uk_ticket_sale UNIQUE (performance_id, seat_id),
    CONSTRAINT fk_ticket_booking FOREIGN KEY (booking_id, performance_id, seat_id)
        REFERENCES booking (id, performance_id, seat_id)
);
