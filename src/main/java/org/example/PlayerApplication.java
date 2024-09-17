package org.example;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * This is the entry-point of the application which facades the communication between two players.
 */
public class PlayerApplication {
    private static final Duration SHUTDOWN_BUFFER = Duration.ofSeconds(1);

    private static Map<String, String> loadConfig(String[] params) {
        return Arrays
                .stream(params)
                .map(param -> param.split("="))
                .collect(Collectors.toMap(ent -> ent[0].trim(), ent -> ent[1].trim()));
    }

    /**
     * Execute registered shutdown handlers before termination.
     */
    private static void executeShutdowns() throws InterruptedException {
        Thread.sleep(SHUTDOWN_BUFFER);
    }

    private static void runInitiatorAsync(String initiateeHost, int initiateePort, String playerId, CountDownLatch sigTerm) {
        Thread.startVirtualThread(() -> {
            try (var initiator = PlayerV2.initiate(initiateeHost, initiateePort, playerId, sigTerm)) {
                initiator.sendMessage("Greetings").listenAndReply();
            } catch (IOException | InterruptedException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static void runInitiateeAsync(int initiateePort, String playerId, CountDownLatch sigTerm) {
        Thread.startVirtualThread(() -> {
            try {
                PlayerV2.participate(initiateePort, playerId, sigTerm);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static void runSameProcess() {
        try (var player1 = PlayerV1.create("P1"); var player2 = PlayerV1.create("P2")) {
            // bi-direction registration of users
            player1.bind(player2::acceptAndReply);
            player2.bind(player1::acceptAndReply);
            player1.initiate();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        var configs = loadConfig(args); // parse options from command-line arguments
        var isSameProcess = Boolean.valueOf(configs.getOrDefault("isSameProcess", "true"));

        // Same Process scenario
        if (isSameProcess) {
            runSameProcess();
            return;
        }

        // Different Process scenario
        var playerId = configs.getOrDefault("playerId", UUID.randomUUID().toString());
        var isInitiator = Boolean.valueOf(configs.getOrDefault("isInitiator", "false"));
        var initiateePort = Integer.valueOf(configs.getOrDefault("initiateePort", "-1"));
        var initiateeHost = configs.getOrDefault("initiateeHost", "localhost");

        var noOfOperationTypes = 2; // listen and reply operations
        var noOfPlayerSessions = 1; // communicating with only 1 other player
        var noOfMessagesPerPlayerSession = 10; // number of messages per player
        var totalOperations = noOfOperationTypes * noOfPlayerSessions * noOfMessagesPerPlayerSession; // operation limit
        var sigTerm = new CountDownLatch(totalOperations);

        if (isInitiator) {
            runInitiatorAsync(initiateeHost, initiateePort, playerId, sigTerm);
        } else {
            runInitiateeAsync(initiateePort, playerId, sigTerm);
        }

        sigTerm.await(); // wait till all messages have been received
        executeShutdowns();
    }
}