package android.androidVNC;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/** Publishes the socket before DNS/connect, so cancellation can close it promptly. */
final class ConnectionTransport {
    private final int connectTimeout, startupTimeout;
    private Socket socket;
    private boolean closed;

    ConnectionTransport() { this(10000, 10000); }
    ConnectionTransport(int connectTimeout, int startupTimeout) {
        this.connectTimeout = connectTimeout;
        this.startupTimeout = startupTimeout;
    }

    Socket connect(String host, int port, boolean automatic) throws IOException {
        final Socket pending;
        synchronized (this) {
            if (closed || socket != null) throw new SocketException("Connection already closed or started");
            socket = pending = new Socket();
        }
        try {
            // No monitor is held while the resolver or network can block.
            pending.connect(new InetSocketAddress(host, port), automatic ? connectTimeout : 0);
            synchronized (this) {
                if (closed) throw new SocketException("Connection cancelled");
                if (automatic) pending.setSoTimeout(startupTimeout);
            }
            return pending;
        } catch (IOException error) {
            try { pending.close(); } catch (IOException ignored) { }
            throw error;
        } catch (RuntimeException error) {
            try { pending.close(); } catch (IOException ignored) { }
            throw error;
        }
    }

    synchronized void established() throws IOException {
        if (!closed && socket != null) socket.setSoTimeout(0);
    }

    void close() {
        Socket pending;
        synchronized (this) { closed = true; pending = socket; }
        if (pending != null) try { pending.close(); } catch (IOException ignored) { }
    }
}
