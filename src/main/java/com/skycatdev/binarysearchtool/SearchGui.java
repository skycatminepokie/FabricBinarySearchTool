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
    private final ArrayList<Mod> mods = new ArrayList<>();
    private final ArrayList<Mod> workingMods = new ArrayList<>();
    private final ArrayList<Mod> candidateMods = new ArrayList<>();
    private final ArrayList<Mod> testingMods = new ArrayList<>();
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
        bottomPanel.add(failureButton);

        successButton = new JButton("Success");
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
                        dependencies = dependsElement.getAsJsonArray()
                                .asList()
                                .stream()
                                .map((JsonElement::getAsString))
                                .collect(Collectors.toSet());
                    } else {
                        dependencies = Collections.emptySet();
                    }
                    String id = fmjJson.get("id").getAsString();
                    String fileName = possibleModFile.getName();
                    int extensionIndex = fileName.lastIndexOf(".jar") - 1;
                    if (extensionIndex == -1) {
                        extensionIndex = fileName.length();
                    }
                    mods.add(new Mod(id, dependencies, fileName.substring(0, extensionIndex)));
                }
            } catch (IOException e) {
                // TODO: JarFile error
            }
        }
        candidateMods.addAll(mods);
        for (Mod testingMod : testingMods) {
            if (!testingMod.tryDisable(modsPath)) {
                // TODO: Warning with instructions to fix
            }
        }
        searching = true;
        bisect(true, 1);
    }

    /**
     *
     * @param lastSuccessful If the last set was successful (error is gone)
     * @param iteration The iteration of this step
     */
    private void bisect(boolean lastSuccessful, int iteration) {
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
        testingMods.clear();
        // Ready for next step

        // TODO: Choose mods to use

        for (Mod testingMod : testingMods) {
            if (!testingMod.tryEnable(modsPath)) {
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
