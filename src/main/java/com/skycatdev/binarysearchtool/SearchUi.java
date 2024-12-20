package com.skycatdev.binarysearchtool;

import java.util.ArrayList;
import java.util.concurrent.Future;

public interface SearchUi {
    /**
     * Show a dialog with options of what to do.
     * Closes when an option is selected, and runs the callback of the option on another thread
     *
     * @return A future that completes when the option is chosen. Maybe a funny way of doing things, but oh well.
     * @implSpec Must block other ui actions. Must not block the thread it is called on.
     */
    Future<Void> asyncDisplayOption(String title, String text, @SuppressWarnings("unused") MessageType messageType, Option[] options);

    @SuppressWarnings("unused")
    void failure();

    @SuppressWarnings("unused")
    SearchHandler getSearchHandler();

    void initialize(SearchHandler searchHandler);

    /**
     * SearchHandler is finished bisecting and is ready for feedback.
     * It is waiting for you to call success or failure.
     */
    void onBisectFinished();

    void onFinished(ArrayList<Mod> problematicMods);

    void sendInstructions(String instructions);

    void sendNextStepInstructions();

    @SuppressWarnings("unused")
    void start();

    @SuppressWarnings("unused")
    void success();

    /**
     * Update the display of the mod lists. Blocking.
     *
     * @param candidateMods Mods that may be the problem.
     * @param workingMods Mods that are not the problem.
     */
    void updateLists(ArrayList<Mod> candidateMods, ArrayList<Mod> workingMods);

    /**
     * Update the progress of searching. Blocking.
     *
     * @param iterations The number of times bisected.
     * @param maxIterations The max number of times it should take to bisect.
     */
    void updateProgress(int iterations, int maxIterations);
}
