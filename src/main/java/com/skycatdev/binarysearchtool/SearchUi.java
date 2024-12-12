package com.skycatdev.binarysearchtool;

import java.util.ArrayList;
import java.util.concurrent.Future;

public interface SearchUi {
    /**
     * Show a dialog with options of what to do.
     * Closes when an option is selected, and runs the callback of the option on another thread
     * @return A future that completes when the option is chosen. Maybe a funny way of doing things, but oh well.
     * @implSpec Must block other ui actions. Must not block the thread it is called on.
     */
    Future<Void> asyncDisplayOption(String title, String text, MessageType messageType, Option[] options);
    SearchHandler getSearchHandler();

    void onFinished(Mod problematicMod);

    void initialize(SearchHandler searchHandler);

    /**
     * Update the display of the mod lists. Blocking.
     * @param candidateMods
     * @param workingMods
     */
    void updateLists(ArrayList<Mod> candidateMods, ArrayList<Mod> workingMods);

    /**
     * Update the progress of searching. Blocking.
     * @param iterations
     * @param maxIterations
     */
    void updateProgress(int iterations, int maxIterations);
    void sendInstructions(String instructions);

    /**
     * SearchHandler is finished bisecting and is ready for feedback.
     * It is waiting for you to call success or failure.
     */
    void onBisectFinished();
    void start();
    void success();
    void failure();
}
