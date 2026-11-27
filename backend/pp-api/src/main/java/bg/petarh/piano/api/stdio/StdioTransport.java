package bg.petarh.piano.api.stdio;

import bg.petarh.piano.api.rpc.EventSink;
import bg.petarh.piano.api.rpc.RpcServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class StdioTransport {

    private static final Optional<String> END = Optional.empty();

    private final RpcServer server;
    private final BlockingQueue<Optional<String>> outgoing = new LinkedBlockingQueue<>();
    private volatile boolean open = true;
    private Runnable endOfInput = () -> {
    };

    public StdioTransport(RpcServer server) {
        this.server = server;
    }

    public EventSink events() {
        return (method, params) -> {
            if (open) {
                outgoing.add(Optional.of(server.notification(method, params)));
            }
        };
    }

    public void onEndOfInput(Runnable action) {
        this.endOfInput = action;
    }

    public void run(InputStream in, OutputStream out) {
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        Thread writing = Thread.ofPlatform().name("api-writer").daemon().start(() -> write(writer));
        try {
            readRequests(in);
        } finally {
            endOfInput.run();
            open = false;
            outgoing.add(END);
            joinQuietly(writing);
        }
    }

    private void readRequests(InputStream in) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    server.handle(line).ifPresent(response -> outgoing.add(Optional.of(response)));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Reading requests failed", e);
        }
    }

    private void write(Writer writer) {
        try {
            for (Optional<String> line = outgoing.take(); line.isPresent(); line = outgoing.take()) {
                writer.write(line.get());
                writer.write('\n');
                writer.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // The other side has gone away, so there is no one left to tell.
        }
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
