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

    public CliUi() {
    }

    @Override
    public Future<Option> asyncDisplayOption(String title, String text, MessageType messageType, Option[] options) {
        FutureTask<Option> future = new FutureTask<>(() -> {
            System.out.println(text); // TODO allow shorthand
            for (Option option : options) {
                System.out.println(option.name());
            }
            Option chosen = null;
            while (chosen == null) {
                String choice;
                try (Scanner scanner = new Scanner(System.in)) {
                    choice = scanner.nextLine();
                }
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
            return chosen;
        });
        new Thread(future).start();
        return future;
    }

    @Override
    public SearchHandler getSearchHandler() {
        return searchHandler;
    }

    @Override
    public void onFinished(Mod problematicMod) {
        System.out.printf("Finished! The problematic mod was: %s (%s)%n", problematicMod.name(), problematicMod.filename());
    }

    @Override
    public void setSearchHandler(SearchHandler searchHandler) {
        this.searchHandler = searchHandler;
    }

    @Override
    public void updateLists(ArrayList<Mod> candidateMods, ArrayList<Mod> workingMods) {
        // TODO
    }

    @Override
    public void updateProgress(int iterations, int maxIterations) {
        // TODO
    }

    @Override
    public void sendInstructions(String instructions) {
        // TODO
    }

    @Override
    public void start() {
        if (searchHandler != null) {
            searchHandler.discoverMods();
            searchHandler.bisect(true);
        }
    }
}
