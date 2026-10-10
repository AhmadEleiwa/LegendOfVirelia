package org.engine.utils;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Debug {
    private static final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    public static volatile boolean enable = true;

    static {
        Thread loggerThread = new Thread(Debug::processQueue, "LoggerThread");
        loggerThread.setDaemon(true);
        loggerThread.start();
    }

    public static void log(String message) {
        if (!enable)
            return;
        String entry = "[" + LocalDateTime.now() + "] " + message;
        logQueue.offer(entry);
    }

    public static void log(int message) {
        if (!enable)
            return;
        String entry = "[" + LocalDateTime.now() + "] " + String.valueOf(message);
        logQueue.offer(entry);
    }

    private static void processQueue() {
        while (true) {
            try {
                System.out.println(logQueue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
