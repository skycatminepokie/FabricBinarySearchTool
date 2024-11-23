package com.skycatdev.binarysearchtool;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import javax.swing.JButton;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import javax.swing.JSplitPane;
import javax.swing.JProgressBar;
import javax.swing.JTextPane;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class SearchGui extends JFrame {
    public static final Gson GSON = new Gson();
    private final JTextPane instructionsPane;
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

        instructionsPane = new JTextPane();
        instructionsPane.setText("Instructions");
        mainPanel.add(instructionsPane, BorderLayout.EAST);

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
        middlePanel.setLeftComponent(maybeProblemPane);

        notProblemPane = new JTextPane();
        notProblemPane.setText("Not the problem");
        middlePanel.setRightComponent(notProblemPane);

        topPanel = new JPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        folderLabel = new JLabel("mods Folder");
        topPanel.add(folderLabel);

        pathField = new JTextField();
        topPanel.add(pathField);
        pathField.setColumns(50);

        startButton = new JButton("Start");
        startButton.addActionListener(this::onStartButtonPressed);
        topPanel.add(startButton);
    }

    private void onStartButtonPressed(ActionEvent event) {
        // TODO: Don't start searching twice
        try {
            Path inputPath = FileSystems.getDefault().getPath(pathField.getText());
            File inputFile = inputPath.toFile();
            if (inputFile.exists()) {
                if (inputFile.isDirectory()) {
                    modsPath = inputPath;
                } else {
                    // TODO not a directory
                    return;
                }
            } else {
                // TODO directory does not exist
                return;
            }
        } catch (InvalidPathException e) {
            // TODO invalid path
            return;
        }
        // modsPath is initialized
        // populate mods
        File[] possibleModFiles;
        try {
            possibleModFiles = modsPath.toFile().listFiles(file -> file.getPath().endsWith(".jar"));
        } catch (SecurityException e) {
            // TODO: Could not access
            return;
        }
        if (possibleModFiles == null) {
            // TODO: I/O error
            return;
        }
        for (File possibleModFile : possibleModFiles) {
            try (JarFile jarFile = new JarFile(possibleModFile)) {
                JarEntry fmj = jarFile.getJarEntry("fabric.mod.json");
                if (fmj == null) {
                    // TODO: Maybe warn that it's not a mod
                    continue;
                }
                try (InputStream inputStream = jarFile.getInputStream(fmj)) {
                    JsonObject fmjJson = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
                    JsonElement dependsElement = fmjJson.get("depends");
                    Set<String> dependencies;
                    if (dependsElement != null) {
                        dependencies = dependsElement.getAsJsonObject().keySet();
                    } else {
                        dependencies = Collections.emptySet();
                    }
                    String id = fmjJson.get("id").getAsString();
                    String fileName = possibleModFile.getName();
                    int extensionIndex = fileName.lastIndexOf(".jar");
                    if (extensionIndex == -1) {
                        extensionIndex = fileName.length();
                    }
                    mods.add(new Mod(id, dependencies, fileName.substring(0, extensionIndex))); // TODO: Parse JIJ/"provides", probably by having multiple ids per "Mod"
                }
            } catch (IOException e) {
                // TODO: JarFile error
            }
        }
        candidateMods.addAll(mods);
        for (Mod mod : mods) {
            if (!mod.tryDisable(modsPath)) {
                // TODO: Warning with instructions to fix
            }
        }
        searching = true;
        bisect(true);
    }

    /**
     *
     * @param lastSuccessful If the last set was successful (error is gone)
     */
    private void bisect(boolean lastSuccessful) {
        assert modsPath != null;
        if (lastSuccessful) {
            workingMods.addAll(testingMods);
        } else {
            workingMods.addAll(candidateMods);
            workingMods.removeAll(testingMods);
            candidateMods.clear();
            candidateMods.addAll(testingMods);
        }
        for (Mod testingMod : testingMods) {
            if (!testingMod.tryDisable(modsPath)) {
                // TODO: Warning with instructions to fix
            }
        }
        for (Mod dependencyMod : testingDependencies) {
            if (!dependencyMod.tryDisable(modsPath)) {
                // TODO: Warning with instructions to fix
            }
        }
        testingMods.clear();
        testingDependencies.clear();
        // Ready for next step

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
                if (testingMods.stream().anyMatch((testMod) -> testMod.id().equals(dependency))) continue;
                if (testingDependencies.stream().anyMatch((dependencyMod) -> dependencyMod.id().equals(dependency))) continue;

                // Check if it's a candidate
                boolean found = false;
                for (int i = 0; i < candidateMods.size(); i++) {
                    if (candidateMods.get(i).id().equals(dependency)) {
                        testingMods.add(candidateMods.remove(i));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    for (Mod workingMod : workingMods) {
                        if (workingMod.id().equals(dependency)) {
                            testingDependencies.add(workingMod);
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    if (mods.stream().anyMatch((mod1 -> mod1.id().equals(dependency)))) {
                        // TODO: I did an oops, it should be in either testingMods, candidateMods, or working mods
                    } else {
                        // TODO: Missing dependency, you didn't need to binary search, silly.
                    }
                }
            }
        }

        // Enable mods we're using
        for (Mod testingMod : testingMods) {
            if (!testingMod.tryEnable(modsPath)) {
                // TODO: Warning with instructions to fix
            }
        }
        for (Mod dependencyMod : testingDependencies) {
            if (!dependencyMod.tryEnable(modsPath)) {
                // TODO: Warning with instructions to fix
            }
        }

        updateLists();
        updateProgress();
        instructionsPane.setText("Next step is ready! Launch Minecraft, test (or crash), then close it (or crash). If the error is gone, press Success. If it's still there, press Failure.");
    }

    private void updateProgress() {
        // TODO
    }

    private void updateLists() {
        // TODO
    }

    // TODO: When finished, enable all mods

}
