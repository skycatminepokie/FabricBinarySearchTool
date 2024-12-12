package com.skycatdev.binarysearchtool;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class CliUi implements SearchUi {
    /**
     * The SearchHandler this is linked to. Is null if none is linked.
     */
    protected @Nullable SearchHandler searchHandler = null;
    protected Scanner scanner = new Scanner(System.in);
    protected final DialogHandler dialogHandler = new DialogHandler();

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

    @Override
    public void failure() {
        System.out.println("Working...");
        if (getSearchHandler() != null) {
            getSearchHandler().bisect(false);
        }
    }

    @Override
    public SearchHandler getSearchHandler() {
        return searchHandler;
    }

    @Override
    public void initialize(SearchHandler searchHandler) {
        this.searchHandler = searchHandler;
        asyncDisplayOption("", "Ready to start?", MessageType.NONE, new Option[]{new Option("start", this::start)});
    }

    @Override
    public void onFinished(Mod problematicMod) {
        assert searchHandler != null : "searchHandler should be the one calling, why is it null?";
        System.out.printf("Finished! The problematic mod was: %s (%s)%n", problematicMod.name(), problematicMod.filename());
        searchHandler.onUiClosing();
    }

    @Override
    public void sendInstructions(String instructions) {
        System.out.println(instructions);
    }

    @Override
    public void onBisectFinished() {
        new Thread(() -> blockingDisplayOption("Is the problem fixed?", new Option[]{new Option("Yes", this::success), new Option("No", this::failure)})).start();
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

    private void blockingDisplayOption(String text, Option[] options) {
        System.out.println(text); // TODO allow shorthand
        for (Option option : options) {
            System.out.println(option.name());
        }
        Option chosen = null;
        while (chosen == null) {
            String choice;
            choice = scanner.nextLine();
            for (Option option : options) {
                if (option.name().equals(choice)) {
                    chosen = option;
                    if (option.callback() != null) {
                        option.callback().run();
                    }
                    break;
                }
            }
            if (chosen == null) {
                System.out.println("That was not an option!");
            }
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
}
