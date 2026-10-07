package kr.noco.qticket.infra.redis.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SeatRealtimeEventTest {

    @Test
    void givenValidEvent_whenJsonRoundTrip_thenAllFieldsArePreserved() {
        SeatRealtimeEvent event = event();
        JsonMapper mapper = JsonMapper.builder().build();

        SeatRealtimeEvent decoded = mapper.readValue(
                mapper.writeValueAsString(event), SeatRealtimeEvent.class);

        assertThat(decoded).isEqualTo(event);
    }

    @Test
    void givenInvalidIdentifier_whenCreated_thenRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SeatRealtimeEvent(
                UUID.randomUUID(), 0L, 2L, SeatRealtimeEvent.Type.HOLD, Instant.now()));
        assertThatIllegalArgumentException().isThrownBy(() -> new SeatRealtimeEvent(
                UUID.randomUUID(), 1L, 0L, SeatRealtimeEvent.Type.HOLD, Instant.now()));
        assertThatIllegalArgumentException().isThrownBy(() -> new SeatRealtimeEvent(
                UUID.randomUUID(), null, 2L, SeatRealtimeEvent.Type.HOLD, Instant.now()));
        assertThatIllegalArgumentException().isThrownBy(() -> new SeatRealtimeEvent(
                UUID.randomUUID(), 1L, null, SeatRealtimeEvent.Type.HOLD, Instant.now()));
    }

    @Test
    void givenNullRequiredField_whenCreated_thenRejected() {
        assertThatNullPointerException().isThrownBy(() -> new SeatRealtimeEvent(
                null, 1L, 2L, SeatRealtimeEvent.Type.HOLD, Instant.now()));
        assertThatNullPointerException().isThrownBy(() -> new SeatRealtimeEvent(
                UUID.randomUUID(), 1L, 2L, null, Instant.now()));
        assertThatNullPointerException().isThrownBy(() -> new SeatRealtimeEvent(
                UUID.randomUUID(), 1L, 2L, SeatRealtimeEvent.Type.HOLD, null));
    }

    static SeatRealtimeEvent event() {
        return new SeatRealtimeEvent(UUID.randomUUID(), 7L, 42L, SeatRealtimeEvent.Type.HOLD,
                Instant.parse("2026-10-06T00:00:00Z"));
    }
}
