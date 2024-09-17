package org.example;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * This class Provides one-to-one communication capabilities between two players using the
 * bounded-callback/bounded-recursion pattern.
 *
 * Note:
 * - Since this class is cheap to create we can create multiple instances to simulate one-to-many capability.
 */
public class PlayerV1 implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(PlayerV1.class.getName());

    private Consumer<String> channel; // Other player's callback function
    private int messageCounter = 0; // number Of messages written
    private final int MAX_MESSAGE_COUNT = 10; // maximum number of messages to be written
    private final String id; // player id

    private PlayerV1(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public static PlayerV1 create(String id) {
        return new PlayerV1(id);
    }

    /**
     * Unidirectional link between two users.
     */
    public void bind(Consumer<String> channel) {
        LOG.info("Creating communication channel for %s".formatted(id));
        this.channel = Objects.requireNonNull(channel);
    }

    public void initiate() {
        var sentMessage = prepareMessage("Greetings");
        channel.accept(sentMessage);
    }

    protected String prepareMessage(String message) {
        return message + (++messageCounter);
    }

    /**
     * Listens for a message and reply the caller if rate limit is not reached.
     */
    public void acceptAndReply(String receivedMessage) {
        LOG.info("Player id: %s received message: %s".formatted(id, receivedMessage));
        if (messageCounter < MAX_MESSAGE_COUNT) {
            var sentMessage = prepareMessage(receivedMessage);
            Objects.requireNonNull(channel).accept(sentMessage);
        }
    }

    @Override
    public void close() {
        LOG.info("Closing communication channel for Player: %s".formatted(id));
    }
}