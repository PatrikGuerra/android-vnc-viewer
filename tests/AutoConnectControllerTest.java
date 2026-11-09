package android.androidVNC;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public final class AutoConnectControllerTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        AutoConnectController c = new AutoConnectController();
        check(c.state == AutoConnectController.State.IDLE && c.tryStart(0) == 0, "Disabled by default");
        check(c.intervalSeconds == 10, "Default interval");
        c.begin(42, 10, 5000);
        check(c.connectionId == 42, "Selected profile ID must be retained");
        check(c.tryStart(5000, false) == 0, "Do not start in background");
        long first = c.tryStart(5000);
        check(first > 0 && c.tryStart(5000) == 0, "Immediate first attempt without overlap");
        c.failed(first, true, 7000);
        check(c.tryStart(20000, false) == 0, "Suspend retries in background");
        check(c.tryStart(16999) == 0 && c.secondsRemaining(16999) == 1, "Wait full retry interval");
        long second = c.tryStart(17000);
        check(second > first, "Retry has its own token");
        c.failed(first, true, 18000);
        check(c.state == AutoConnectController.State.CONNECTING, "Ignore stale failure");
        c.connected(second);
        check(c.state == AutoConnectController.State.CONNECTED && c.tryStart(20000) == 0, "No attempts while connected");
        c.failed(second, true, 25000);
        check(c.state == AutoConnectController.State.WAITING && c.due == 35000, "Retry after session drops");
        c.cancel();
        c.connected(second);
        c.failed(second, true, 40000);
        check(c.state == AutoConnectController.State.CANCELLED && c.tryStart(90000) == 0, "Cancel waiting invalidates callbacks");
        c.begin(43, 1, 100000);
        long third = c.tryStart(100000);
        c.cancel();
        c.failed(third, true, 100001);
        check(c.state == AutoConnectController.State.CANCELLED, "Cancel connecting is final for this opening");
        c.begin(43, 3600, 200000);
        long fourth = c.tryStart(200000);
        c.connected(fourth);
        c.cancel();
        c.failed(fourth, true, 201000);
        check(c.tryStart(Long.MAX_VALUE) == 0, "Stop reconnect while connected");
        c.begin(43, 10, 300000);
        c.failed(c.tryStart(300000), false, 300001);
        check(c.state == AutoConnectController.State.CANCELLED, "Terminal error stops cycle");
        c.begin(43, 10, 400000);
        check(c.tryStart(400000) > 0, "New explicit opening rearms the cycle");
        int[][] invalid = {{0,10}, {1,0}, {1,3601}};
        for (int[] settings : invalid) {
            try { c.begin(settings[0], settings[1], 0); throw new AssertionError("Invalid configuration accepted"); }
            catch (IllegalArgumentException expected) { }
        }
        check(AutoConnectContract.isRetryable(new ConnectException("Connection refused")), "Refused connection is retryable");
        check(AutoConnectContract.isRetryable(new SocketTimeoutException("Timed out")), "Timeout is retryable");
        check(AutoConnectContract.isRetryable(new UnknownHostException("DNS unavailable")), "Temporary DNS failure is retryable");
        check(!AutoConnectContract.isRetryable(new IOException("authentication failed")), "Do not repeat rejected password");
        check(!AutoConnectContract.isRetryable(new OutOfMemoryError()), "Do not loop on low memory");
        check(!AutoConnectContract.isRetryable(new IllegalArgumentException("Bad profile")), "Invalid profile stops");
        System.out.println("PASS: automatic connection selection, timing, reconnection, cancellation, stale results and terminal errors");
    }
}
