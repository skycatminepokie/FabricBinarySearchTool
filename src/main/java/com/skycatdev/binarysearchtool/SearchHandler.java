package com.skycatdev.binarysearchtool;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SearchHandler {
    private static final Option[] DO_NOTHING_OPTION = {new Option("OK", null)};
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
    private final ArrayList<Mod> forceEnabled = new ArrayList<>();
    private final Path modsPath;
    private final SearchUi ui;
    private final ModSearchComparator modComparator = new ModSearchComparator();
    private int maxIterations = 0;
    private int iterations = 0;
    private boolean finished = false;
    private boolean madeShutdownHook = false;

    private SearchHandler(Path modsPath, SearchUi ui) {
        this.modsPath = modsPath;
        this.ui = ui;
    }

    /**
     * @param inputPath A validated path to the mods folder.
     * @param ui        The frontend ui to use.
     * @return A new {@link SearchHandler}.
     * @throws IllegalArgumentException If the file at {@code inputPath} does not exist.
     * @implSpec {@link SearchUi#initialize(SearchHandler)} has NOT been called.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static SearchHandler createWithUi(Path inputPath, SearchUi ui) throws IllegalArgumentException {
        SearchHandler searchHandler = new SearchHandler(inputPath, ui);
        searchHandler.discoverMods();
        ui.initialize(searchHandler);
        searchHandler.addShutdownHook();
        return searchHandler;
    }

    private static <T> void rotateList(ArrayList<T> list, int rotation) {
        for (int i = 0; i < rotation; i++) {
            list.addLast(list.removeFirst());
        }
    }

    private void addDeps(Mod mod) {
        Main.log("Resolving dependencies");
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
                    addDeps(workingMod);
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (int i = 0; i < candidateMods.size(); i++) {
                    if (candidateMods.get(i).ids().contains(dependency)) {
                        Mod candidateMod = candidateMods.remove(i);
                        testingMods.add(candidateMod);
                        addDeps(candidateMod);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                if (mods.stream().anyMatch((mod1 -> mod1.ids().contains(dependency)))) {
                    Main.log("Mod gone wot");
                    ui.asyncDisplayOption("Mod gone wot",
                            "I did an oops, it should be in either testingMods, candidateMods, or working mods.\n" +
                            "Please report this, unless you messed with files. In that case, have an angry face >:(",
                            MessageType.ERROR,
                            new Option[]{
                                    new Option("I got an angry face", this::onFatalError),
                                    new Option("I didn't get an angry face", this::onFatalError)});
                } else {
                    ui.asyncDisplayOption("Missing dependency",
                            "You seem to be missing a dependency - %s. Fabric should've told you this (or you're using dependency overrides). If I'm wrong, report this.".formatted(dependency),
                            MessageType.WARNING,
                            DO_NOTHING_OPTION
                    );
                    Main.log("Missing a dependency");
                }
            }
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean addForceEnabled(Mod mod) {
        if (forceEnabled.contains(mod)) {
            return false;
        }
        forceEnabled.add(mod);
        workingMods.add(mod);
        candidateMods.remove(mod);
        return true;
    }

    private void addShutdownHook() {
        if (!madeShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (!finished) {
                    mods.forEach((mod) -> mod.tryEnable(modsPath));
                }
            }));
            madeShutdownHook = true;
        }
    }

    /**
     * @param lastSuccessful If the last set was successful (error is gone)
     */
    public void bisect(boolean lastSuccessful) {
        Main.log("Top of bisect");
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
            finished = true;
            ui.updateLists(candidateMods, workingMods);
            ui.updateProgress(iterations, maxIterations);
            ui.onFinished(candidateMods);
            enableAll(mods);
            return;
        } else {
            if (candidateMods.isEmpty()) {
                ui.asyncDisplayOption("Uh-oh!", "Oops! There's no candidate mods. Get help using the help button in the main window.", MessageType.ERROR, new Option[]{new Option("OK", this::onFatalError)});
                return;
            }
        }
        Main.log("Beginning bisection");
        // Choose mods to use
        int rotation = 0;
        boolean choseMods = false;
        while (rotation < candidateMods.size()) {
            candidateMods.sort(modComparator);
            rotateList(candidateMods, rotation);
            ui.updateLists(candidateMods, workingMods);
            ui.updateProgress(iterations, maxIterations);
            int candidatesSize = candidateMods.size();
            while (testingMods.size() < candidatesSize / 2) {
                // Add the mod to the testing set, remove it from the candidate set
                Mod mod = candidateMods.removeFirst();
                testingMods.add(mod);
                Main.log("Added mod " + mod.name());
                addDeps(mod);
            }
            if (!candidateMods.isEmpty()) {
                choseMods = true;
                break;
            }
            Main.log("Unhelpful search set, rotating");
            candidateMods.addAll(testingMods);
            testingMods.clear();
            testingDependencies.clear();
            rotation++;
        }
        if (!choseMods) {
            iterations++;
            finished = true;
            ui.updateLists(candidateMods, workingMods);
            ui.updateProgress(iterations, maxIterations);
            ui.onFinished(candidateMods);
            enableAll(mods);
            return;
        }

        for (Mod mod : forceEnabled) {
            enableMod(mod);
            Main.log("Added force-enabled mod " + mod.name());
            addDeps(mod);
        }
        // Enable mods we're using
        enableAll(testingMods);
        enableAll(testingDependencies);
        ui.sendInstructions("Next step is ready! Launch Minecraft, test (or crash), then close it (or crash). If the error is gone, press Success. If it's still there, press Failure.");
        ui.onBisectFinished();
        Main.log("Bottom of bisect");
    }

    private void disableAll(ArrayList<Mod> mods) {
        for (Mod testingMod : mods) {
            disableMod(testingMod);
        }
    }

    private void disableMod(Mod mod) {
        assert modsPath != null;
        if (!mod.tryDisable(modsPath)) {
            try {
                ui.asyncDisplayOption("Disable failed", "Couldn't disable \"%s\". Make sure Minecraft is closed.".formatted(mod.name()), MessageType.WARNING, new Option[]{
                        new Option("Abort", this::onFatalError),
                        new Option("Try again", () -> disableMod(mod))
                }).get();
            } catch (ExecutionException | InterruptedException e) {
                ui.asyncDisplayOption("Uh-oh", "Failed to disable gracefully", MessageType.ERROR, new Option[]{new Option("OK, cya", this::onFatalError)});
            }
        }
    }

    private void discoverMods() {
        // If something here goes wrong, the user should end up being forced to quit.
        Main.log("Discovering mods");
        try {
            Mod.loadDependencyOverrides(modsPath.getParent().resolve("config").resolve("fabric_loader_dependencies.json").toFile());
        } catch (IOException e) {
            Main.log("Failed to load dependency overrides.");
            Main.log(e.getMessage());
            ui.asyncDisplayOption("Dep override failure", "Could not load dependency overrides", MessageType.INFO, DO_NOTHING_OPTION);
            return;
        }
        // modsPath is initialized
        // populate mods
        File[] possibleModFiles;
        try {
            possibleModFiles = modsPath.toFile().listFiles(file -> file.getPath().endsWith(".jar"));
        } catch (SecurityException e) {
            Main.log("Could not access file when discovering");
            ui.asyncDisplayOption("Could not access file", "Could not access a file in the provided path. Make sure Minecraft is closed and try again.", MessageType.INFO, new Option[]{new Option("That's not good, I'll try again", this::onFatalError)});
            return;
        }
        if (possibleModFiles == null) {
            Main.log("Problems with possible mod files");
            ui.asyncDisplayOption("Problems with possible mod files", "There were problems trying to find your mods. Make sure Minecraft is closed and try again.", MessageType.INFO, new Option[]{new Option("That's not good, I'll try again", this::onFatalError)});
            return;
        }
        for (File possibleModFile : possibleModFiles) {
            try (JarFile jarFile = new JarFile(possibleModFile)) {
                Mod parsedMod = parseMod(jarFile);
                if (parsedMod != null) {
                    mods.add(parsedMod);
                }
            } catch (IOException e) {
                Main.log("Problems trying to read mods");
                mods.clear();
                ui.asyncDisplayOption("Problems", "There were problems trying to read your mods.", MessageType.INFO, new Option[]{new Option("That's not good, I'll try again", this::onFatalError)});
                return;
            }
        }
        candidateMods.addAll(mods);
        if (candidateMods.isEmpty()) {
            Main.log("No mods found");
            ui.asyncDisplayOption("Can't find mods", "Couldn't find any mods. Make sure you've got the right folder, and you have Fabric mods in it.", MessageType.INFO, new Option[]{new Option("That's not good, I'll try again", this::onFatalError)});
            return;
        }

        disableAll(mods);
        maxIterations = (int) Math.ceil(Math.log10(mods.size()) / Math.log10(2.0d));
    }

    private void enableAll(ArrayList<Mod> mods) {
        for (Mod mod : mods) {
            enableMod(mod);
        }
    }

    private void enableMod(Mod mod) {
        assert modsPath != null;
        if (!mod.tryEnable(modsPath)) {
            try {
                ui.asyncDisplayOption("Enable failed", "Couldn't enable \"%s\". Make sure Minecraft is closed.".formatted(mod.name()), MessageType.WARNING, new Option[]{
                        new Option("Abort", this::onFatalError),
                        new Option("Try again", () -> enableMod(mod))
                }).get();
            } catch (ExecutionException | InterruptedException e) {
                ui.asyncDisplayOption("Uh-oh", "Failed to enable gracefully", MessageType.ERROR, new Option[]{new Option("OK, cya", this::onFatalError)});
            }
        }
    }

    public ArrayList<Mod> getMods() {
        return mods;
    }

    /**
     * Call after the error has been acknowledged by a button press.
     */
    private void onFatalError() {
        mods.forEach((mod) -> {
            assert modsPath != null;
            mod.tryEnable(modsPath);
        });
        System.exit(1);
    }

    private @Nullable Mod parseMod(JarFile jarFile) throws IOException {
        Main.log("Parsing mod");
        JarEntry fmj = jarFile.getJarEntry("fabric.mod.json");
        Main.log("Found fmj");
        if (fmj == null) { // No fmj
            return null;
        }
        Main.log("Making input stream");
        // jarIs is at the beginning of the fmj
        try (InputStream inputStream = jarFile.getInputStream(fmj)) {
            Main.log("Input stream made");
            JsonObject fmjJson = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
            // Name
            Main.log("Getting name...");
            JsonElement nameElement = fmjJson.get("name");
            String name = null;
            if (nameElement != null) {
                name = nameElement.getAsString();
            }

            // Ids
            Main.log("Getting ids...");
            String mainId = fmjJson.get("id").getAsString();
            if (name == null) {
                name = mainId;
            }
            Set<String> ids = new HashSet<>();
            ids.add(mainId);
            JsonElement provides = fmjJson.get("provides");
            if (provides != null) {
                provides.getAsJsonArray().forEach((element) -> ids.add(element.getAsString()));
            }

            // Deps
            Main.log("Getting deps...");
            JsonElement dependsElement = fmjJson.get("depends");
            HashSet<String> dependencies;
            if (dependsElement != null) {
                dependencies = new HashSet<>(dependsElement.getAsJsonObject().keySet());
            } else {
                dependencies = new HashSet<>();
            }

            // Filename
            Main.log("Getting file name...");
            String fileName = jarFile.getName();
            int extensionIndex = fileName.lastIndexOf(".jar");
            if (extensionIndex == -1) {
                ui.asyncDisplayOption("Say what now?", "Couldn't find .jar extension for the jar that definitely had a .jar extension. Wot?", MessageType.ERROR, DO_NOTHING_OPTION);
                throw new IOException("Couldn't find .jar extension for the jar that definitely had a .jar extension. Wot?");
            }

            // JIJs
            Main.log("Getting JIJs");
            JsonElement jars = fmjJson.get("jars");
            if (jars != null) {
                for (JsonElement element : jars.getAsJsonArray()) {
                    String jijPath = element.getAsJsonObject().get("file").getAsString();
                    File tempFile = Files.createTempFile("skycatdevbinarysearchtool", ".jar").toFile();
                    tempFile.deleteOnExit();
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
            /*
            Ignore all deps that are provided by itself. For example, modules in Fabric API depend on other modules in Fabric API.
            These deps of deps collect up, making Fabric API have many dependencies, despite all of them being supplied by itself.
            This line fixes that.
             */
            dependencies.removeAll(ids);

            return new Mod(name, mainId, ids, dependencies, fileName.substring(0, extensionIndex));
        }
    }
}
