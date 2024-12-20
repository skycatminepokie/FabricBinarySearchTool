package com.skycatdev.binarysearchtool;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class CliUi implements SearchUi {
    protected final DialogHandler dialogHandler = new DialogHandler();
    /**
     * The SearchHandler this is linked to. Is null if none is linked.
     */
    protected @Nullable SearchHandler searchHandler = null;
    protected final Scanner scanner = new Scanner(System.in);

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
        asyncDisplayOption("", "Ready to start?", MessageType.NONE, new Option[]{new Option("start", this::start)});
    }

    @Override
    public void onBisectFinished() {
        asyncDisplayOption("", "Is the problem fixed?", MessageType.NONE, new Option[]{new Option("Yes", this::success), new Option("No", this::failure)});
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
    }

    @Override
    public void sendInstructions(String instructions) {
        System.out.println(instructions);
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
        // TODO: Add a way to query the search handler, that's what the cli will be doing
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
