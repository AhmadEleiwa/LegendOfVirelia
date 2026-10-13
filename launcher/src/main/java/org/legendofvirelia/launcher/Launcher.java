package org.legendofvirelia.launcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileSystemView;

public final class Launcher {
    private static final Color BACKGROUND = new Color(15, 19, 27);
    private static final Color SURFACE = new Color(24, 30, 41);
    private static final Color MUTED = new Color(151, 163, 181);
    private static final Color TEXT = new Color(237, 242, 250);
    private static final Color ACCENT = new Color(66, 193, 161);

    private final JFrame frame = new JFrame("Legend of Virelia");
    private final JLabel projectLabel = new JLabel();
    private final JLabel statusLabel = new JLabel("Ready to launch");
    private final JTextArea output = new JTextArea();
    private final JButton playButton = new JButton("BUILD & PLAY");
    private final JButton stopButton = new JButton("STOP");
    private final JButton folderButton = new JButton("CHANGE FOLDER");
    private Path projectRoot;
    private Process gameProcess;
    private SwingWorker<Integer, String> launchWorker;

    private Launcher() {
        projectRoot = findProjectRoot();
        buildWindow();
        updateProjectLabel();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Launcher().show());
    }

    private void buildWindow() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(680, 520));
        frame.setSize(760, 600);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BACKGROUND);
        frame.setLayout(new BorderLayout(0, 0));

        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(SURFACE);
        header.setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
        JLabel emblem = new JLabel("V", SwingConstants.CENTER);
        emblem.setOpaque(true);
        emblem.setBackground(ACCENT);
        emblem.setForeground(BACKGROUND);
        emblem.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        emblem.setPreferredSize(new Dimension(48, 48));
        header.add(emblem, BorderLayout.WEST);

        JPanel titleArea = new JPanel(new GridBagLayout());
        titleArea.setOpaque(false);
        GridBagConstraints titleConstraints = new GridBagConstraints();
        titleConstraints.gridx = 0;
        titleConstraints.anchor = GridBagConstraints.WEST;
        titleConstraints.insets = new Insets(0, 0, 3, 0);
        JLabel title = new JLabel("LEGEND OF VIRELIA");
        title.setForeground(TEXT);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        titleArea.add(title, titleConstraints);
        titleConstraints.gridy = 1;
        JLabel subtitle = new JLabel("Voxel adventure  |  Desktop edition");
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        titleArea.add(subtitle, titleConstraints);
        header.add(titleArea, BorderLayout.CENTER);
        frame.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        JPanel launchCard = new JPanel(new BorderLayout(12, 14));
        launchCard.setBackground(SURFACE);
        launchCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel sectionTitle = new JLabel("GAME PROJECT");
        sectionTitle.setForeground(MUTED);
        sectionTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        launchCard.add(sectionTitle, BorderLayout.NORTH);

        projectLabel.setForeground(TEXT);
        projectLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        launchCard.add(projectLabel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        styleButton(playButton, ACCENT, BACKGROUND);
        styleButton(stopButton, new Color(75, 87, 105), TEXT);
        styleButton(folderButton, new Color(42, 51, 66), TEXT);
        stopButton.setEnabled(false);
        JPanel leftActions = new JPanel(new BorderLayout(10, 0));
        leftActions.setOpaque(false);
        leftActions.add(playButton, BorderLayout.WEST);
        leftActions.add(stopButton, BorderLayout.CENTER);
        actions.add(leftActions, BorderLayout.WEST);
        actions.add(folderButton, BorderLayout.EAST);
        launchCard.add(actions, BorderLayout.SOUTH);

        JPanel outputCard = new JPanel(new BorderLayout(0, 10));
        outputCard.setBackground(SURFACE);
        outputCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JPanel outputHeader = new JPanel(new BorderLayout());
        outputHeader.setOpaque(false);
        JLabel outputTitle = new JLabel("LAUNCH OUTPUT");
        outputTitle.setForeground(MUTED);
        outputTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        outputHeader.add(outputTitle, BorderLayout.WEST);
        statusLabel.setForeground(ACCENT);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        outputHeader.add(statusLabel, BorderLayout.EAST);
        outputCard.add(outputHeader, BorderLayout.NORTH);

        output.setEditable(false);
        output.setBackground(new Color(11, 14, 20));
        output.setForeground(new Color(200, 214, 229));
        output.setCaretColor(TEXT);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(output);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(43, 52, 67)));
        outputCard.add(scrollPane, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);
        center.add(launchCard, BorderLayout.NORTH);
        center.add(outputCard, BorderLayout.CENTER);
        content.add(center, BorderLayout.CENTER);
        frame.add(content, BorderLayout.CENTER);

        JLabel footer = new JLabel("The launcher builds and runs the game using its Gradle wrapper.");
        footer.setForeground(MUTED);
        footer.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        footer.setBorder(BorderFactory.createEmptyBorder(0, 28, 18, 28));
        frame.add(footer, BorderLayout.SOUTH);

        playButton.addActionListener(event -> launchGame());
        stopButton.addActionListener(event -> stopGame());
        folderButton.addActionListener(event -> chooseProjectFolder());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (gameProcess != null && gameProcess.isAlive()) {
                    stopGame();
                }
                frame.dispose();
            }
        });
    }

    private void show() {
        frame.setVisible(true);
        if (projectRoot == null) {
            appendOutput("Choose the game project folder. It must contain build.gradle and gradlew.bat.");
            chooseProjectFolder();
        }
    }

    private void launchGame() {
        if (projectRoot == null || !isGameProject(projectRoot)) {
            chooseProjectFolder();
            if (projectRoot == null)
                return;
        }
        if (launchWorker != null && !launchWorker.isDone())
            return;

        output.setText("");
        appendOutput("Project: " + projectRoot);
        appendOutput("Building and starting the game...");
        statusLabel.setText("Building");
        playButton.setEnabled(false);
        folderButton.setEnabled(false);
        stopButton.setEnabled(true);
        launchWorker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                List<String> command = gradleCommand(projectRoot);
                publish("$ " + String.join(" ", command));
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(projectRoot.toFile());
                builder.redirectErrorStream(true);
                gameProcess = builder.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        gameProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null)
                        publish(line);
                }
                return gameProcess.waitFor();
            }

            @Override
            protected void process(List<String> lines) {
                for (String line : lines)
                    appendOutput(line);
                if (statusLabel.getText().equals("Building")
                        && lines.stream().anyMatch(line -> line.contains("> Task :run"))) {
                    statusLabel.setText("Starting game");
                }
            }

            @Override
            protected void done() {
                gameProcess = null;
                playButton.setEnabled(true);
                folderButton.setEnabled(true);
                stopButton.setEnabled(false);
                try {
                    int exitCode = get();
                    if (exitCode == 0) {
                        statusLabel.setText("Game closed");
                        appendOutput("Game process exited.");
                    } else {
                        statusLabel.setText("Launch failed");
                        appendOutput("Gradle exited with code " + exitCode + ".");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Launch interrupted");
                    appendOutput("Launcher was interrupted.");
                } catch (ExecutionException exception) {
                    statusLabel.setText("Launch failed");
                    appendOutput("Could not start the game: " + exception.getCause().getMessage());
                }
            }
        };
        launchWorker.execute();
    }

    private void stopGame() {
        Process process = gameProcess;
        if (process == null || !process.isAlive())
            return;
        statusLabel.setText("Stopping");
        appendOutput("Stopping game...");
        try {
            process.descendants().forEach(ProcessHandle::destroy);
            process.destroy();
            if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private void chooseProjectFolder() {
        JFileChooser chooser = new JFileChooser(projectRoot == null
                ? FileSystemView.getFileSystemView().getDefaultDirectory()
                : projectRoot.toFile());
        chooser.setDialogTitle("Select the LegendOfVirelia project folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION)
            return;

        Path selected = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        if (!isGameProject(selected)) {
            JOptionPane.showMessageDialog(frame,
                    "The selected folder must contain build.gradle and "
                            + (isWindows() ? "gradlew.bat." : "gradlew."),
                    "Invalid game folder", JOptionPane.ERROR_MESSAGE);
            return;
        }
        projectRoot = selected;
        updateProjectLabel();
        appendOutput("Selected project: " + projectRoot);
    }

    private void updateProjectLabel() {
        projectLabel.setText(projectRoot == null
                ? "No game folder selected"
                : "<html>" + escapeHtml(projectRoot.toString()) + "</html>");
    }

    private void appendOutput(String line) {
        output.append(line + System.lineSeparator());
        if (output.getDocument().getLength() > 100_000)
            output.replaceRange("", 0, output.getDocument().getLength() - 75_000);
        output.setCaretPosition(output.getDocument().getLength());
    }

    private static List<String> gradleCommand(Path root) {
        if (isWindows())
            return List.of("cmd.exe", "/d", "/c", "call \"" + root.resolve("gradlew.bat")
                    + "\" run --console=plain");
        return List.of(root.resolve("gradlew").toString(), "run", "--console=plain");
    }

    private static Path findProjectRoot() {
        List<Path> candidates = new ArrayList<>();
        try {
            candidates.add(Path.of(Launcher.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI()));
        } catch (URISyntaxException exception) {
            System.err.println("Could not resolve launcher location: " + exception.getMessage());
        }
        candidates.add(Path.of("").toAbsolutePath());
        for (Path candidate : candidates) {
            Path current = Files.isDirectory(candidate) ? candidate : candidate.getParent();
            while (current != null) {
                if (isGameProject(current))
                    return current;
                current = current.getParent();
            }
        }
        return null;
    }

    private static boolean isGameProject(Path path) {
        return path != null && Files.isRegularFile(path.resolve("build.gradle"))
                && Files.isRegularFile(path.resolve(isWindows() ? "gradlew.bat" : "gradlew"));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void styleButton(JButton button, Color background, Color foreground) {
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
    }
}
