package kr.noco.qticket.app.realtime;

public interface SeatRealtimePublisherPort {

    void publish(SeatRealtimeEvent event);
}
