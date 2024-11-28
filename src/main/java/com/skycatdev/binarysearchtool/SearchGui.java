package com.skycatdev.binarysearchtool;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
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
        middlePanel.setLeftComponent(maybeProblemPane);

        notProblemPane = new JTextPane();
        notProblemPane.setText("Not the problem");
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
        // TODO: Don't start searching twice
        try {
            Path inputPath = FileSystems.getDefault().getPath(pathField.getText());
            File inputFile = inputPath.toFile();
            if (inputFile.exists()) {
                if (inputFile.isDirectory()) {
                    modsPath = inputPath;
                } else {
                    // TODO not a directory
                    System.out.println("Not a directory");
                    return;
                }
            } else {
                // TODO directory does not exist
                System.out.println("Directory does not exist");
                return;
            }
        } catch (InvalidPathException e) {
            // TODO invalid path
            System.out.println("Invalid path");
            return;
        }
        // modsPath is initialized
        // populate mods
        File[] possibleModFiles;
        try {
            possibleModFiles = modsPath.toFile().listFiles(file -> file.getPath().endsWith(".jar"));
        } catch (SecurityException e) {
            // TODO: Could not access
            System.out.println("Could not access");
            return;
        }
        if (possibleModFiles == null) {
            // TODO: I/O error
            System.out.println("I/O error");
            return;
        }
        for (File possibleModFile : possibleModFiles) {
            try (JarFile jarFile = new JarFile(possibleModFile)) {
                Mod parsedMod = parseMod(jarFile);
                if (parsedMod != null) {
                    mods.add(parsedMod);
                }
            } catch (IOException e) {
                // TODO: JarFile error
                System.out.println("JarFile error");
            }
        }
        candidateMods.addAll(mods);
        // TODO: Make sure there's at least one mod
        for (Mod mod : mods) {
            if (!mod.tryDisable(modsPath)) {
                // TODO: Warning with instructions to fix
                System.out.printf("Couldn't disable mod %s\n", mod.filename());
            }
        }
        searching = true;
        startButton.setEnabled(false);
        bisect(true);
    }

    private @Nullable Mod parseMod(JarFile jarFile) throws IOException {
        JarEntry fmj = jarFile.getJarEntry("fabric.mod.json");
        if (fmj == null) { // No fmj
            // TODO: Maybe warn that it's not a mod
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
                // TODO
                System.out.println("Couldn't find .jar extension for the jar that definitely had a .jar extension. Wot?");
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
            if (!testingMod.tryDisable(modsPath)) {
                // TODO: Warning with instructions to fix
                System.out.printf("Couldn't disabled testing mod %s\n", testingMod.filename());
            }
        }
        for (Mod dependencyMod : testingDependencies) {
            if (!dependencyMod.tryDisable(modsPath)) {
                // TODO: Warning with instructions to fix
                System.out.printf("Couldn't disable mod %s\n", dependencyMod.filename());
            }
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
            JDialog finished = createDialog("Finished! The problematic mod is: " + candidateMods.getFirst().filename(), "OK", this::onFinished);
            finished.setVisible(true);
            return;
        } else {
            if (candidateMods.isEmpty()) {
                // TODO: Oops
                JDialog oops = createDialog("Oops! There's no candidate mods. Get help using the help button in the main window.", "OK", this::onFatalError);
                oops.setVisible(true);
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
                        // TODO: I did an oops, it should be in either testingMods, candidateMods, or working mods
                        System.out.println("I did an oops, it should be in either testingMods, candidateMods, or working mods");
                    } else {
                        // TODO: Missing dependency, you didn't need to binary search, silly.
                        System.out.println("Missing dependency, you didn't need to binary search, silly.");
                    }
                }
            }
        }

        // Enable mods we're using
        for (Mod testingMod : testingMods) {
            if (!testingMod.tryEnable(modsPath)) {
                // TODO: Warning with instructions to fix
                System.out.printf("Couldn't enable testing mod %s\n", testingMod.filename());
            }
        }
        for (Mod dependencyMod : testingDependencies) {
            if (!dependencyMod.tryEnable(modsPath)) {
                // TODO: Warning with instructions to fix
                System.out.printf("Couldn't enabled dependency %s\n", dependencyMod.filename());
            }
        }

        updateLists();
        updateProgress();
        instructionsArea.setText("Next step is ready! Launch Minecraft, test (or crash), then close it (or crash). If the error is gone, press Success. If it's still there, press Failure.");
    }

    /**
     * Call after the error has been acknowledged by a button press.
     */
    private void onFatalError(ActionEvent actionEvent) {
        // TODO Enable all mods, close.
    }

    private void onFinished(ActionEvent actionEvent) {
        // TODO
        mods.forEach((mod) -> {
            assert modsPath != null;
            mod.tryEnable(modsPath);
        });
    }

    private JDialog createDialog(String text, String buttonText, ActionListener buttonAction) {
        JDialog dialog = new JDialog();
        dialog.setLayout(new BorderLayout());
        dialog.add(new JLabel(text), BorderLayout.CENTER);
        JButton button = new JButton(buttonText);
        button.addActionListener(buttonAction);
        dialog.add(button, BorderLayout.SOUTH);
        dialog.pack();
        return dialog;
    }

    private void updateProgress() {
        // TODO
    }

    private void updateLists() {
        // TODO
    }

}
