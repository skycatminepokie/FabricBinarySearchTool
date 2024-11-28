package com.skycatdev.binarysearchtool;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.function.BiConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SearchGui extends JFrame {
    public static final Gson GSON = new Gson();
    private final JTextArea instructionsArea;
    private final JPanel bottomPanel;
    private final JButton undoButton;
    private final JButton helpButton;
    private final JProgressBar progressBar;
    private final JButton failureButton;
    private final JButton successButton;
    private final JSplitPane middlePanel;
    private final JTextPane maybeProblemPane;
    private final JTextPane notProblemPane;
    private final JPanel topPanel;
    private final JLabel folderLabel;
    private final JButton startButton;
    private JPanel mainPanel;
    private JTextField pathField;
    private @Nullable Path modsPath;
    /**
     * A list of all mods from the beginning
     */
    private final ArrayList<Mod> mods = new ArrayList<>();
    /**
     * Mods that have been verified as working
     */
    private final ArrayList<Mod> workingMods = new ArrayList<>();
    /**
     * Mods that may or may not be the problem
     */
    private final ArrayList<Mod> candidateMods = new ArrayList<>();
    /**
     * Mods that we are checking for problems
     */
    private final ArrayList<Mod> testingMods = new ArrayList<>();
    /**
     * Mods that are verified as working AND are dependencies of testingMods
     */
    private final ArrayList<Mod> testingDependencies = new ArrayList<>();
    private boolean searching = false;
    /**
     * Number of mods we're waiting to enable or disable. A hack.
     */
    int manualWaiting = 0;

    /**
     * Create the frame.
     */
    public SearchGui() {
        setTitle("Skycat's Fabric Binary Search Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 800, 500);
        mainPanel = new JPanel();
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        setContentPane(mainPanel);
        mainPanel.setLayout(new BorderLayout(0, 0));

        instructionsArea = new JTextArea();
        instructionsArea.setText("Paste the path to your mods folder above. Make sure Minecraft is closed, then click start.");
        instructionsArea.setBorder(new EmptyBorder(5,5,5,5));
        instructionsArea.setWrapStyleWord(true);
        instructionsArea.setLineWrap(true);
        instructionsArea.setSize(200, 100);
        instructionsArea.setEditable(false);
        mainPanel.add(instructionsArea, BorderLayout.EAST);

        bottomPanel = new JPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        undoButton = new JButton("Undo");
        bottomPanel.add(undoButton);

        helpButton = new JButton("Help");
        bottomPanel.add(helpButton);

        progressBar = new JProgressBar();
        progressBar.setMaximum(100);
        bottomPanel.add(progressBar);

        failureButton = new JButton("Failure");
        failureButton.addActionListener((event) -> bisect(false));
        bottomPanel.add(failureButton);

        successButton = new JButton("Success");
        successButton.addActionListener((event) -> bisect(true));
        bottomPanel.add(successButton);

        middlePanel = new JSplitPane();
        mainPanel.add(middlePanel, BorderLayout.CENTER);

        maybeProblemPane = new JTextPane();
        maybeProblemPane.setText("Might be the problem");
        maybeProblemPane.setEnabled(false);
        middlePanel.setLeftComponent(maybeProblemPane);

        notProblemPane = new JTextPane();
        notProblemPane.setText("Not the problem");
        notProblemPane.setEnabled(false);
        middlePanel.setRightComponent(notProblemPane);

        topPanel = new JPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        folderLabel = new JLabel("mods folder");
        topPanel.add(folderLabel);

        pathField = new JTextField();
        topPanel.add(pathField);
        pathField.setColumns(50);

        startButton = new JButton("Start");
        startButton.addActionListener(this::onStartButtonPressed);
        topPanel.add(startButton);
    }

    private void onStartButtonPressed(ActionEvent event) {
        try {
            Path inputPath = FileSystems.getDefault().getPath(pathField.getText());
            File inputFile = inputPath.toFile();
            if (inputFile.exists()) {
                if (inputFile.isDirectory()) {
                    modsPath = inputPath;
                } else {
                    showDialog("The path you've specified is not a directory. Please try again.", "OK", (dialog, event1) -> dialog.setVisible(false));
                    return;
                }
            } else {
                showDialog("The directory you've specified does not exist. Please try again.", "OK", (dialog, event1) -> dialog.setVisible(false));
                return;
            }
        } catch (InvalidPathException e) {
            showDialog("The path you've specified is invalid. Please try again.", "OK", (dialog, event1) -> dialog.setVisible(false));
            return;
        }
        // modsPath is initialized
        // populate mods
        File[] possibleModFiles;
        try {
            possibleModFiles = modsPath.toFile().listFiles(file -> file.getPath().endsWith(".jar"));
        } catch (SecurityException e) {
            showDialog("Could not access a file in the provided path. Make sure Minecraft is closed and try again.", "OK", (dialog, event1) -> dialog.setVisible(false));
            return;
        }
        if (possibleModFiles == null) {
            showDialog("There were problems trying to find your mods. Make sure Minecraft is closed and try again.", "OK", (dialog, event1) -> dialog.setVisible(false));
            return;
        }
        for (File possibleModFile : possibleModFiles) {
            try (JarFile jarFile = new JarFile(possibleModFile)) {
                Mod parsedMod = parseMod(jarFile);
                if (parsedMod != null) {
                    mods.add(parsedMod);
                }
            } catch (IOException e) {
                showDialog("There were problems trying to read your mods.", "OK", (dialog, event1) -> dialog.setVisible(false));
                mods.clear();
                return;
            }
        }
        candidateMods.addAll(mods);
        if (candidateMods.isEmpty()) {
            showDialog("Couldn't find any mods. Make sure you've got the right folder, and you have Fabric mods in it.", "OK", (dialog, event1) -> dialog.setVisible(false));
            return;
        }

        for (Mod mod : mods) {
            disableMod(mod);
        }
        searching = true;
        startButton.setEnabled(false);
        bisect(true);
    }

    private @Nullable Mod parseMod(JarFile jarFile) throws IOException {
        JarEntry fmj = jarFile.getJarEntry("fabric.mod.json");
        if (fmj == null) { // No fmj
            return null;
        }
        // jarIs is at the beginning of the fmj
        try (InputStream inputStream = jarFile.getInputStream(fmj)) {
            JsonObject fmjJson = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
            // Ids
            String id = fmjJson.get("id").getAsString();
            Set<String> ids = new HashSet<>();
            ids.add(id);
            JsonElement provides = fmjJson.get("provides");
            if (provides != null) {
                provides.getAsJsonArray().forEach((element) -> ids.add(element.getAsString()));
            }

            // Deps
            JsonElement dependsElement = fmjJson.get("depends");
            HashSet<String> dependencies;
            if (dependsElement != null) {
                dependencies = new HashSet<>(dependsElement.getAsJsonObject().keySet());
            } else {
                dependencies = new HashSet<>();
            }

            // Filename
            String fileName = jarFile.getName();
            int extensionIndex = fileName.lastIndexOf(".jar");
            if (extensionIndex == -1) {
                showDialog("Couldn't find .jar extension for the jar that definitely had a .jar extension. Wot?", "Abort", this::onFatalError);
                throw new IOException("Couldn't find .jar extension for the jar that definitely had a .jar extension. Wot?");
            }

            JsonElement jars = fmjJson.get("jars");
            if (jars != null) {
                for (JsonElement element : jars.getAsJsonArray()) {
                    String jijPath = element.getAsJsonObject().get("file").getAsString();
                    File tempFile = Files.createTempFile("skycatdevbinarysearchtool", ".jar").toFile();
                    try (FileOutputStream tempFileOutputStream = new FileOutputStream(tempFile)) {
                        jarFile.getInputStream(jarFile.getJarEntry(jijPath)).transferTo(tempFileOutputStream);
                    }
                    Mod jij = parseMod(new JarFile(tempFile));
                    if (jij != null) {
                        ids.addAll(jij.ids());
                        dependencies.addAll(jij.dependencies());
                    }
                }
            }

            return new Mod(ids, dependencies, fileName.substring(0, extensionIndex));
        }
    }

    /**
     *
     * @param lastSuccessful If the last set was successful (error is gone)
     */
    private void bisect(boolean lastSuccessful) {
        assert modsPath != null;
        // Disabled all the previously-enabled mods
        for (Mod testingMod : testingMods) {
            disableMod(testingMod);
        }
        for (Mod dependencyMod : testingDependencies) {
            disableMod(dependencyMod);
        }

        // Decide which set contains the problem
        if (lastSuccessful) {
            workingMods.addAll(testingMods);
        } else {
            workingMods.addAll(candidateMods);
            candidateMods.clear();
            candidateMods.addAll(testingMods);
        }
        testingMods.clear();
        testingDependencies.clear();
        // Ready for next step
        if (candidateMods.size() == 1) {
            showDialog("Finished! The problematic mod is: " + candidateMods.getFirst().filename(), "OK", this::onFinished);
            return;
        } else {
            if (candidateMods.isEmpty()) {
                showDialog("Oops! There's no candidate mods. Get help using the help button in the main window.", "OK", this::onFatalError);
                return;
            }
        }

        // Choose mods to use
        candidateMods.sort(Comparator.comparing((mod) -> mod.dependencies().size()));
        int previousSize = candidateMods.size();
        while (testingMods.size() < previousSize / 2) {
            // Add the mod to the testing set, remove it from the candidate set
            Mod mod = candidateMods.removeFirst();
            testingMods.add(mod);

            // Add dependencies
            for (String dependency : mod.dependencies()) {
                if (dependency.equals("minecraft") || dependency.equals("fabricloader") || dependency.equals("java")) continue;
                // Check if we already have it
                if (testingMods.stream().anyMatch((testMod) -> testMod.ids().contains(dependency))) continue;
                if (testingDependencies.stream().anyMatch((dependencyMod) -> dependencyMod.ids().contains(dependency))) continue;

                // Check if it's a candidate
                boolean found = false;
                for (int i = 0; i < candidateMods.size(); i++) {
                    if (candidateMods.get(i).ids().contains(dependency)) {
                        testingMods.add(candidateMods.remove(i));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    for (Mod workingMod : workingMods) {
                        if (workingMod.ids().contains(dependency)) {
                            testingDependencies.add(workingMod);
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    if (mods.stream().anyMatch((mod1 -> mod1.ids().contains(dependency)))) {
                        showDialog("I did an oops, it should be in either testingMods, candidateMods, or working mods.\n" +
                                   "Please report this, unless you messed with files. In that case, have an angry face >:(", "OK", this::onFatalError);
                    } else {
                        showDialog("You seem to be missing a dependency - %s\nFabric should've told you this.\nIf I'm wrong, report this.".formatted(dependency), "OK", this::onFatalError);
                    }
                }
            }
        }
        // Enable mods we're using
        for (Mod testingMod : testingMods) {
            enableMod(testingMod);
        }
        for (Mod dependencyMod : testingDependencies) {
            enableMod(dependencyMod);
        }
        updateLists();
        updateProgress();
        instructionsArea.setText("Next step is ready! Launch Minecraft, test (or crash), then close it (or crash). If the error is gone, press Success. If it's still there, press Failure.");
    }

    /**
     * Call after the error has been acknowledged by a button press.
     */
    private void onFatalError(JDialog dialog, ActionEvent actionEvent) {
        // TODO Enable all mods, close.
        mods.forEach((mod) -> {
            assert modsPath != null;
            mod.tryEnable(modsPath);
        });
        System.exit(1);
    }

    private void onFinished(JDialog dialog, ActionEvent actionEvent) {
        mods.forEach(this::enableMod);
        dialog.setVisible(false);
        failureButton.setEnabled(false); // TODO: Toggle on when undoing
        successButton.setEnabled(false);
    }

    private JDialog showDialog(String text, String buttonText, BiConsumer<JDialog, ActionEvent> buttonAction) {
        JDialog dialog = new JDialog();
        dialog.setLayout(new BorderLayout());
        dialog.add(new JLabel(text), BorderLayout.CENTER);
        JButton button = new JButton(buttonText);
        button.addActionListener((event) -> buttonAction.accept(dialog, event));
        dialog.add(button, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setVisible(true);
        return dialog;
    }

    /**
     * @implNote Non-blocking. Use carefully.
     */
    private void disableMod(Mod mod) { // TODO: Make this blocking
        assert modsPath != null;
        if (!mod.tryDisable(modsPath)) {
            manualWaiting++;
            JDialog dialog = new JDialog();
            dialog.setLayout(new BorderLayout());
            dialog.add(new JLabel("Failed to disable mod %s".formatted(mod.filename())), BorderLayout.CENTER);
            JButton button = new JButton("Try again");
            button.addActionListener((event) -> {
                disableMod(mod);
                manualWaiting--;
            });
            dialog.add(button, BorderLayout.SOUTH);
            dialog.pack();
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        }
    }

    /**
     * @implNote Non-blocking. Use carefully.
     */
    private void enableMod(Mod mod) { // TODO: Make this blocking
        assert modsPath != null;
        // Try nicely
        if (!mod.tryEnable(modsPath)) {
            // Force it
            JDialog dialog = new JDialog();
            dialog.setLayout(new BorderLayout());
            dialog.add(new JLabel("Failed to enable mod %s".formatted(mod.filename())), BorderLayout.CENTER);
            JButton button = new JButton("Try again");
            button.addActionListener((event) -> {
                enableMod(mod);
            });
            dialog.add(button, BorderLayout.SOUTH);
            dialog.pack();
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        }
    }

    private void updateProgress() {
        // TODO
    }

    private void updateLists() {
        StringBuilder maybeProblem = new StringBuilder();
        for (Mod candidate : candidateMods) {
            maybeProblem.append(candidate.filename());
            maybeProblem.append('\n');
        }
        maybeProblemPane.setText(maybeProblem.toString());
        StringBuilder notProblem = new StringBuilder();
        for (Mod candidate : workingMods) {
            notProblem.append(candidate.filename());
            notProblem.append('\n');
        }
        notProblemPane.setText(notProblem.toString());
    }

}
