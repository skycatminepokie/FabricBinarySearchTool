package com.skycatdev.binarysearchtool;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SearchHandler {
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
    private final Path modsPath;
    private final SearchGui gui;
    private int maxIterations = 0;
    private int iterations = 0;

    private SearchHandler(Path modsPath, SearchGui gui) {
        this.modsPath = modsPath;
        this.gui = gui;
    }

    public static @Nullable SearchHandler createAndBind(Path inputPath, SearchGui gui) {
        try {
            File inputFile = inputPath.toFile();
            if (inputFile.exists()) {
                if (inputFile.isDirectory()) {
                    SearchHandler searchHandler = new SearchHandler(inputPath, gui);
                    SwingUtilities.invokeLater(() -> gui.searchHandler = searchHandler);
                    searchHandler.discoverMods();
                    searchHandler.bisect(true);
                    return searchHandler;
                } else {
                    showDialog("The path you've specified is not a directory. Please try again.", "OK", (dialog, event1) -> dialog.setVisible(false));
                    return null;
                }
            } else {
                showDialog("The directory you've specified does not exist. Please try again.", "OK", (dialog, event1) -> dialog.setVisible(false));
                return null;
            }
        } catch (InvalidPathException e) {
            showDialog("The path you've specified is invalid. Please try again.", "OK", (dialog, event1) -> dialog.setVisible(false));
            return null;
        }
    }

    private static void showDialog(String text, String buttonText, BiConsumer<JDialog, ActionEvent> buttonAction) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog();
            dialog.setLayout(new BorderLayout());
            dialog.add(new JLabel(text), BorderLayout.CENTER);
            JButton button = new JButton(buttonText);
            button.addActionListener((event) -> buttonAction.accept(dialog, event));
            dialog.add(button, BorderLayout.SOUTH);
            dialog.pack();
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.setVisible(true);
        });
    }

    /**
     * @param lastSuccessful If the last set was successful (error is gone)
     */
    public void bisect(boolean lastSuccessful) {
        assert modsPath != null;
        // Disabled all the previously-enabled mods
        disableAll(testingMods);
        disableAll(testingDependencies);

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
        iterations++;

        // Ready for next step
        if (candidateMods.size() == 1) {
            iterations++;
            showDialog("Finished! The problematic mod is: " + candidateMods.getFirst().name(), "OK", this::onFinished);
            SwingUtilities.invokeLater(() -> {
                gui.updateLists(candidateMods, workingMods);
                gui.updateProgress(iterations, maxIterations);
            });
            return;
        } else {
            if (candidateMods.isEmpty()) {
                showDialog("Oops! There's no candidate mods. Get help using the help button in the main window.", "OK", this::onFatalError);
                return;
            }
        }

        // Choose mods to use
        candidateMods.sort(Comparator.comparing((mod) -> mod.dependencies().size()));
        try {
            SwingUtilities.invokeAndWait(() -> {
                gui.updateLists(candidateMods, workingMods);
                gui.updateProgress(iterations, maxIterations);
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        int previousSize = candidateMods.size();
        while (testingMods.size() < previousSize / 2) {
            // Add the mod to the testing set, remove it from the candidate set
            Mod mod = candidateMods.removeFirst();
            testingMods.add(mod);

            // Add dependencies
            for (String dependency : mod.dependencies()) {
                if (dependency.equals("minecraft") || dependency.equals("fabricloader") || dependency.equals("java"))
                    continue;
                // Check if we already have it
                if (testingMods.stream().anyMatch((testMod) -> testMod.ids().contains(dependency))) continue;
                if (testingDependencies.stream().anyMatch((dependencyMod) -> dependencyMod.ids().contains(dependency)))
                    continue;

                // Check if it's a candidate
                boolean found = false;
                for (Mod workingMod : workingMods) {
                    if (workingMod.ids().contains(dependency)) {
                        testingDependencies.add(workingMod);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    for (int i = 0; i < candidateMods.size(); i++) {
                        if (candidateMods.get(i).ids().contains(dependency)) {
                            testingMods.add(candidateMods.remove(i));
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
                        showDialog("You seem to be missing a dependency - %s\nFabric should've told you this.\nIf I'm wrong, report this.".formatted(dependency), "OK", this::onFatalError); // TODO: This doesn't account for dependency overrides
                    }
                }
            }
        }
        // Enable mods we're using
        enableAll(testingMods);
        enableAll(testingDependencies);
        SwingUtilities.invokeLater(() -> gui.instructionsArea.setText("Next step is ready! Launch Minecraft, test (or crash), then close it (or crash). If the error is gone, press Success. If it's still there, press Failure."));
    }

    private void disableAll(ArrayList<Mod> testingMods) {
        for (Mod testingMod : testingMods) {
            disableMod(testingMod);
        }
    }

    /**
     * @implNote Non-blocking. Use carefully.
     */
    private void disableMod(Mod mod) {
        assert modsPath != null;
        if (!mod.tryDisable(modsPath)) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    JDialog dialog = new JDialog(gui, true);
                    dialog.setLayout(new BorderLayout());
                    dialog.add(new JLabel("Couldn't disable \"%s\". Make sure Minecraft is closed.".formatted(mod.name())), BorderLayout.CENTER);

                    JPanel buttonPanel = new JPanel();
                    buttonPanel.setLayout(new FlowLayout());
                    dialog.add(buttonPanel, BorderLayout.SOUTH);

                    JButton abortButton = new JButton("Abort");
                    abortButton.addActionListener((event) -> onFatalError(dialog, event));
                    buttonPanel.add(abortButton);

                    //noinspection ExtractMethodRecommender
                    JButton tryAgainButton = new JButton("Try again");
                    tryAgainButton.addActionListener((event) -> {
                        tryAgainButton.setText("Trying again...");
                        tryAgainButton.setEnabled(false);
                        if (!mod.tryDisable(modsPath)) {
                            tryAgainButton.setText("Try again");
                            tryAgainButton.setEnabled(true);
                        } else {
                            dialog.setVisible(false);
                            dialog.dispose();
                        }
                    });
                    buttonPanel.add(tryAgainButton);

                    dialog.pack();
                    dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                    dialog.setVisible(true);
                });
            } catch (InterruptedException | InvocationTargetException e) {
                SwingUtilities.invokeLater(() -> {
                    JDialog dialog = new JDialog(gui, true);
                    dialog.setLayout(new BorderLayout());
                    dialog.add(new JLabel("Failed to gracefully fail to disable a mod."), BorderLayout.CENTER);
                    JButton button = new JButton("OK bye");
                    button.addActionListener((event) -> onFatalError(dialog, event));
                    dialog.add(button, BorderLayout.SOUTH);
                    dialog.pack();
                    dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                    dialog.setVisible(true);
                });
            }
        }
    }

    public void discoverMods() {
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

        disableAll(mods);
        maxIterations = (int) Math.ceil(Math.log10(mods.size()) / Math.log10(2.0d));
    }

    private void enableAll(ArrayList<Mod> testingMods) {
        for (Mod testingMod : testingMods) {
            enableMod(testingMod);
        }
    }

    /**
     * @implNote Non-blocking. Use carefully.
     */
    private void enableMod(Mod mod) {
        assert modsPath != null;
        if (!mod.tryEnable(modsPath)) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    JDialog dialog = new JDialog(gui, true);
                    dialog.setLayout(new BorderLayout());
                    dialog.add(new JLabel("Couldn't enable \"%s\". Make sure Minecraft is closed.".formatted(mod.name())), BorderLayout.CENTER);

                    JPanel buttonPanel = new JPanel();
                    buttonPanel.setLayout(new FlowLayout());
                    dialog.add(buttonPanel, BorderLayout.SOUTH);

                    JButton abortButton = new JButton("Abort");
                    abortButton.addActionListener((event) -> onFatalError(dialog, event));
                    buttonPanel.add(abortButton);

                    //noinspection ExtractMethodRecommender
                    JButton tryAgainButton = new JButton("Try again");
                    tryAgainButton.addActionListener((event) -> {
                        tryAgainButton.setText("Trying again...");
                        tryAgainButton.setEnabled(false);
                        if (!mod.tryEnable(modsPath)) {
                            tryAgainButton.setText("Try again");
                            tryAgainButton.setEnabled(true);
                        } else {
                            dialog.setVisible(false);
                            dialog.dispose();
                        }
                    });
                    buttonPanel.add(tryAgainButton);

                    dialog.pack();
                    dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                    dialog.setVisible(true);
                });
            } catch (InterruptedException | InvocationTargetException e) {
                SwingUtilities.invokeLater(() -> {
                    JDialog dialog = new JDialog(gui, true);
                    dialog.setLayout(new BorderLayout());
                    dialog.add(new JLabel("Failed to gracefully fail to enable a mod."), BorderLayout.CENTER);
                    JButton button = new JButton("OK bye");
                    button.addActionListener((event) -> onFatalError(dialog, event));
                    dialog.add(button, BorderLayout.SOUTH);
                    dialog.pack();
                    dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                    dialog.setVisible(true);
                });
            }
        }
    }

    /**
     * Call after the error has been acknowledged by a button press.
     */
    private void onFatalError(JDialog dialog, ActionEvent actionEvent) {
        mods.forEach((mod) -> {
            assert modsPath != null;
            mod.tryEnable(modsPath);
        });
        System.exit(1);
    }

    private void onFinished(JDialog dialog, ActionEvent actionEvent) {
        mods.forEach(this::enableMod);
        dialog.setVisible(false);
        gui.failureButton.setEnabled(false); // TODO: Toggle on when undoing
        gui.successButton.setEnabled(false);
    }

    private @Nullable Mod parseMod(JarFile jarFile) throws IOException {
        JarEntry fmj = jarFile.getJarEntry("fabric.mod.json");
        if (fmj == null) { // No fmj
            return null;
        }
        // jarIs is at the beginning of the fmj
        try (InputStream inputStream = jarFile.getInputStream(fmj)) {
            JsonObject fmjJson = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
            // Name
            JsonElement nameElement = fmjJson.get("name");
            String name = null;
            if (nameElement != null) {
                name = nameElement.getAsString();
            }

            // Ids
            String id = fmjJson.get("id").getAsString();
            if (name == null) {
                name = id;
            }
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

            return new Mod(name, ids, dependencies, fileName.substring(0, extensionIndex));
        }
    }
}
