package android.androidVNC;

import java.io.IOException;
import java.util.Locale;

final class AutoConnectContract {
    static final int REQUEST = 700;
    static final int RETRY = 1;
    static final int TERMINAL = 2;
    static final int CONNECTED = 3;
    static final int CANCELLED = 0;
    static final String TOKEN = "android.androidVNC.auto.token";
    static final String EVENTS = "android.androidVNC.auto.events";
    static final String ERROR = "android.androidVNC.auto.error";

    static boolean isRetryable(Throwable error) {
        String message = error.getMessage();
        message = message == null ? "" : message.toLowerCase(Locale.US);
        if (message.contains("authentication") || message.contains("password")
                || message.contains("security")) return false;
        return error instanceof IOException;
    }

    private AutoConnectContract() { }
}
