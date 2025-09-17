package org.engine.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Debug {
    private static final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private static final Path LOG_FILE = Paths.get("logs.txt");
    public static boolean enable = true;
    static {
        // Start a background thread to consume logs
        Thread loggerThread = new Thread(Debug::processQueue, "LoggerThread");
        loggerThread.setDaemon(true);
        loggerThread.start();
    }

    public static void log(String message) {
        if(! enable)
            return;
        String entry = "[" + LocalDateTime.now() + "] " + message;
        logQueue.offer(entry);
    }
    public static void log(int message) {
        if(! enable)
            return;
        String entry = "[" + LocalDateTime.now() + "] " + String.valueOf(message);
        logQueue.offer(entry);
    }
    private static void processQueue() {
        if(! enable)
            return;
        try {
            // Ensure file exists
            if (!Files.exists(LOG_FILE)) {
                Files.createFile(LOG_FILE);
            }
        } catch (IOException ignored) {}

        while (true) {
            try {
                String msg = logQueue.take(); // waits if empty
                // print to console asynchronously
                System.out.println(msg);
                // append to file asynchronously
                Files.writeString(LOG_FILE, msg + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
        }
    }
}
