package org.example;

import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/**
 * This class provides one-to-one and many-to-many communication capabilities between multiple players.
 */
public class PlayerV2 implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(PlayerV2.class.getName());
    private static final Duration DELAY = Duration.ofMillis(500); // 0.5 second interval between retries
    private static final int MAX_RETRIES = 120; // retry twice/per second for a minute

    private int messageCounter = 0; // number Of messages written
    private final String id; // player id
    private final Socket client;
    private final CountDownLatch sigTerm; // termination signal
    private final BufferedReader reader;
    private final BufferedWriter writer;

    private PlayerV2(Socket client, String id, CountDownLatch sigTerm) throws IOException {
        this.client = client;
        this.id = id;
        this.sigTerm = sigTerm;
        this.reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
    }

    /**
     * This binds allows a Player to listen and bind on/to a port and reply to messages.
     */
    public static void participate(int port, String id, CountDownLatch sigTerm) throws IOException {
        // Websocket/Non-blocking Selector-ServerSocketChannel would be idea but significantly complex
        try (var server = new ServerSocket(port)) {
            LOG.info("Player: %s is listening at port: %d".formatted(id, port));
            while (sigTerm.getCount() > 0) {
                // Making this async and reactive would be optimal but significantly complex e.g. synchronization
                try (var session = new PlayerV2(server.accept(), id, sigTerm)) {
                    session.listenAndReply();
                }
            }
        }
    }

    /**
     * This lets a Player send/receive messages to a hosting Player.
     */
    public static PlayerV2 initiate(String host, int port, String id, CountDownLatch sigTerm) throws IOException, InterruptedException {
        LOG.info("Player: %s is initiating host: %s port: %d".formatted(id, host, port));
        for (var retries = 0; retries < MAX_RETRIES; ++retries) {
            try {
                return new PlayerV2(new Socket(host, port), id, sigTerm);
            } catch (ConnectException exception) {
                Thread.sleep(DELAY); // Exponential back-off would be ideal here
            }
        }
        // caller can retry layer with this exception
        throw new ConnectException("Unable to Connect with host: %s at port: %s".formatted(host, port));
    }

    public PlayerV2 sendMessage(String message) throws IOException {
        var sentMessage = prepareMessage(message);
        writer.write(sentMessage);
        writer.newLine();
        writer.flush();

        LOG.info("Player: %s sent message: %s to host: %s port: %s".formatted(id, sentMessage, getHost(), getPort()));
        sigTerm.countDown();
        return this;
    }

    protected String prepareMessage(String message) {
        return message + (++messageCounter);
    }

    public String listen() throws IOException {
        var receivedMessage = reader.readLine();

        LOG.info("Player: %s received: %s from host:%s port:%s".formatted(id, receivedMessage, getHost(), getPort()));
        sigTerm.countDown();
        return receivedMessage;
    }

    protected boolean shouldListenOrReply() {
        return sigTerm.getCount() > 0;
    }

    public PlayerV2 listenAndReply() throws IOException {
        if (!shouldListenOrReply()) {
            return this;
        }

        var message = listen();
        if (shouldListenOrReply()) {
            sendMessage(message).listenAndReply();
        }

        return this;
    }

    private String getHost() {
        return client.getInetAddress().getHostName();
    }

    private int getPort() {
        return client.getPort();
    }

    @Override
    public void close() throws IOException {
        LOG.info("Player: %s closing session with host:%s port:%s".formatted(id, getHost(), getPort()));
        client.close();
    }
}