package android.androidVNC;

/** Clock-driven state machine; no Android dependency or network work on the UI thread. */
final class AutoConnectController {
    enum State { IDLE, CONNECTING, CONNECTED, WAITING, CANCELLED }
    State state = State.IDLE;
    long token;
    long due;
    long connectionId;
    int intervalSeconds = 10;

    void begin(long id, int interval, long now) {
        if (id <= 0 || interval < 1 || interval > 3600) throw new IllegalArgumentException();
        token++;
        connectionId = id;
        intervalSeconds = interval;
        due = now;
        state = State.WAITING;
    }

    long tryStart(long now) {
        return tryStart(now, true);
    }

    long tryStart(long now, boolean foreground) {
        if (!foreground || state != State.WAITING || now < due) return 0;
        state = State.CONNECTING;
        return ++token;
    }

    void connected(long attempt) {
        if (attempt == token && state == State.CONNECTING) state = State.CONNECTED;
    }

    void failed(long attempt, boolean retry, long now) {
        if (attempt != token || (state != State.CONNECTING && state != State.CONNECTED)) return;
        state = retry ? State.WAITING : State.CANCELLED;
        due = now + intervalSeconds * 1000L;
    }

    void cancel() {
        token++;
        state = State.CANCELLED;
    }

    long secondsRemaining(long now) {
        return Math.max(0, (due - now + 999) / 1000);
    }
}
