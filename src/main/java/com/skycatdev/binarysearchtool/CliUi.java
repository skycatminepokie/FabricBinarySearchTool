package com.skycatdev.binarysearchtool;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class CliUi implements SearchUi {
    protected final DialogHandler dialogHandler = new DialogHandler();
    protected final Scanner scanner = new Scanner(System.in);
    /**
     * The SearchHandler this is linked to. Is null if none is linked.
     */
    protected @Nullable SearchHandler searchHandler = null;

    public CliUi() {
    }

    @Override
    public Future<Void> asyncDisplayOption(String title, String text, MessageType messageType, Option[] options) {
        FutureTask<Void> future = new FutureTask<>(() -> {
            blockingDisplayOption(text, options);
            return null;
        });
        synchronized (dialogHandler) {
            dialogHandler.display(future);
        }
        return future;
    }

    private void blockingDisplayOption(String text, Option[] options) {
        System.out.println(text);
        for (int i = 0; i < options.length; i++) {
            Option option = options[i];
            System.out.println(option.name() + " [" + (i + 1) + "]");
        }
        Option chosen = null;
        while (chosen == null) {
            String input = scanner.nextLine();
            int optionNumber = -1;
            try {
                optionNumber = Integer.parseInt(input);
            } catch (NumberFormatException ignored) {

            }
            if (optionNumber > 0 && optionNumber <= options.length) {
                chosen = options[optionNumber - 1];
                break;
            }
            for (Option option : options) {
                if (option.name().equals(input)) {
                    chosen = option;
                    break;
                }
            }
            if (chosen == null) {
                System.out.println("That was not an option!");
            }
        }
        if (chosen.callback() != null) {
            chosen.callback().run();
        }
    }

    private void displayStartMenu() {
        asyncDisplayOption("", "Ready to start?", MessageType.NONE, new Option[]{new Option("start", this::start), new Option("advanced", this::openAdvancedOptions)});
    }

    @Override
    public void failure() {
        System.out.println("Working...");
        if (getSearchHandler() != null) {
            getSearchHandler().bisect(false);
        }
    }

    @Override
    public @Nullable SearchHandler getSearchHandler() {
        return searchHandler;
    }

    @Override
    public void initialize(SearchHandler searchHandler) {
        this.searchHandler = searchHandler;
        displayStartMenu();
    }

    @Override
    public void onBisectFinished() {
        showBisectMenu();
    }

    private void showBisectMenu() {
        asyncDisplayOption("", "Is the problem fixed?", MessageType.NONE, new Option[]{
                new Option("Yes", this::success),
                new Option("No", this::failure),
                new Option("List", () -> {
                    showLists();
                    showBisectMenu();
                })
        });
    }

    private void showLists() {
        if (getSearchHandler() == null) {
            asyncDisplayOption("", "SearchHandler was null when trying to display lists. Please report this.", MessageType.ERROR, new Option[]{new Option("OK", null)});
            return;
        }
        System.out.println("Might be the problem:");
        for (Mod mod : getSearchHandler().getCandidateMods()){
            System.out.println(mod.name());
        }
        System.out.println();
        System.out.println("Not the problem:");
        for (Mod mod: getSearchHandler().getWorkingMods()) {
            System.out.println(mod.name());
        }
        System.out.println();
        System.out.println("Mods we're testing now:");
        for (Mod mod : getSearchHandler().getTestingMods()) {
            System.out.println(mod.name());
        }
    }

    @Override
    public void onFinished(ArrayList<Mod> problematicMods) {
        assert searchHandler != null : "searchHandler should be the one calling, why is it null?";
        if (problematicMods.size() == 1) {
            System.out.printf("Finished! The problematic mod was: %s (%s)%n", problematicMods.get(0).name(), problematicMods.get(0).filename());
        } else {
            System.out.println("Finished! The following mods rely on each other, and one is the problem:");
            for (Mod problematicMod : problematicMods) {
                System.out.printf("%s (%s)", problematicMod.name(), problematicMod.filename());
            }
        }
        System.exit(0);
    }

    private void openAdvancedOptions() {
        System.out.println("Advanced options");
        System.out.println("Heh, the only thing we have is force-enabling mods. Type the id of the mod you'd like to force-enable, or \"back\" to go back");
        String id = scanner.nextLine();
        if (id.equals("back")) {
            displayStartMenu();
            return;
        }
        if (getSearchHandler() == null) {
            asyncDisplayOption("", "SearchHandler was not initialized when opening advanced options. Please report this.", MessageType.NONE, new Option[]{new Option("OK", () -> System.exit(-1))});
            return;
        }
        if (getSearchHandler().forceEnable(id)) {
            System.out.println("Success!");
        } else {
            System.out.println("Could not force enable mod. Either it was already force-enabled, or it does not exist.");
        }
        openAdvancedOptions();
    }

    @Override
    public void sendInstructions(String instructions) {
        System.out.println(instructions);
    }

    @Override
    public void sendNextStepInstructions() {
        sendInstructions("Next step is ready! Launch Minecraft, test (or crash), then close it (or crash). Then respond to the prompt.");
    }

    @Override
    public void start() {
        System.out.println("Starting, please wait...");
        if (getSearchHandler() != null) {
            getSearchHandler().bisect(true);
        }
    }

    @Override
    public void success() {
        System.out.println("Working...");
        if (getSearchHandler() != null) {
            getSearchHandler().bisect(true);
        }
    }

    @Override
    public void updateLists(ArrayList<Mod> candidateMods, ArrayList<Mod> workingMods) {
    }

    @Override
    public void updateProgress(int iterations, int maxIterations) {
        // TODO: Add a way to query the search handler, that's what the cli will be doing
    }

    /**
     * Runs every task on a new thread, waiting for each task to be done before starting the next.
     */
    public static class DialogHandler {
        protected final Queue<FutureTask<Void>> tasks = new LinkedList<>();
        protected boolean running = false;

        public synchronized void display(@NotNull FutureTask<Void> dialogFuture) {
            tasks.add(dialogFuture);
            if (!running) {
                runNext();
            }
        }

        private synchronized void runNext() {
            running = true;
            new Thread(() -> {
                FutureTask<Void> future;
                synchronized (DialogHandler.this) {
                    future = tasks.poll();
                    if (future == null) {
                        running = false;
                        return;
                    }
                }
                future.run();
                runNext();
            }).start();
        }
    }
}
