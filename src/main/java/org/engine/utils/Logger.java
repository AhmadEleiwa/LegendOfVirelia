package org.engine.utils;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;


public class Logger {


    private static final String LOG_FILE = "logs.txt"; // will appear in your run folder

    public static synchronized void log(String message, Exception e) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println("[" + LocalDateTime.now() + "] " + message);
            if (e != null) {
                e.printStackTrace(out);
            }
        } catch (IOException io) {
            // ignore logging failures to avoid infinite loop
            Debug.log("Failed to write to log file: " + io.getMessage());
        }
    }

    public static void log(String message) {
        log(message, null);
    }

}
