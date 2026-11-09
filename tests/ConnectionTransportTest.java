package android.androidVNC;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConnectionTransportTest {
    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        ConnectionTransport transport = new ConnectionTransport(500, 100);
        Socket peer = null;
        try {
            final Socket socket = transport.connect("127.0.0.1", server.getLocalPort(), true);
            peer = server.accept();
            if (socket.getSoTimeout() != 100) throw new AssertionError("Startup timeout missing");
            try { socket.getInputStream().read(); throw new AssertionError("Silent handshake did not time out"); }
            catch (SocketTimeoutException expected) { }
            transport.established();
            if (socket.getSoTimeout() != 0) throw new AssertionError("Static desktop must not time out");
            final CountDownLatch started = new CountDownLatch(1);
            final AtomicBoolean unblocked = new AtomicBoolean();
            Thread reader = new Thread(new Runnable() {
                public void run() {
                    started.countDown();
                    try { socket.getInputStream().read(); }
                    catch (IOException closed) { unblocked.set(true); }
                }
            });
            reader.setDaemon(true);
            reader.start();
            if (!started.await(3, TimeUnit.SECONDS)) throw new AssertionError("Reader did not start");
            transport.close();
            reader.join(3000);
            if (reader.isAlive() || !unblocked.get() || !socket.isClosed())
                throw new AssertionError("Cancel did not release blocked socket read");
            try { transport.connect("127.0.0.1", server.getLocalPort(), true); throw new AssertionError("Transport reused after cancel"); }
            catch (IOException expected) { }
        } finally {
            transport.close();
            if (peer != null) peer.close();
            server.close();
        }
        ConnectionTransport cancelled = new ConnectionTransport();
        cancelled.close();
        try { cancelled.connect("127.0.0.1", 1, true); throw new AssertionError("Connected after cancellation"); }
        catch (IOException expected) { }
        ServerSocket unused = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        int refusedPort = unused.getLocalPort();
        unused.close();
        ConnectionTransport refused = new ConnectionTransport(500, 100);
        try { refused.connect("127.0.0.1", refusedPort, true); throw new AssertionError("Closed server unexpectedly accepted"); }
        catch (IOException expected) { }
        finally { refused.close(); }
        ServerSocket manualServer = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        ConnectionTransport manual = new ConnectionTransport(500, 100);
        Socket manualPeer = null;
        try {
            Socket socket = manual.connect("127.0.0.1", manualServer.getLocalPort(), false);
            manualPeer = manualServer.accept();
            if (socket.getSoTimeout() != 0) throw new AssertionError("Manual connection timeout changed");
        } finally {
            manual.close();
            if (manualPeer != null) manualPeer.close();
            manualServer.close();
        }
        System.out.println("PASS: local TCP startup timeout, normal idle session, cancellation and refused server");
    }
}
