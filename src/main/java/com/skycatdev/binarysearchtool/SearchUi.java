package com.skycatdev.binarysearchtool;

import java.util.ArrayList;
import java.util.concurrent.Future;

public interface SearchUi {
    /**
     * Show a dialog with options of what to do.
     * Closes when an option is selected, and runs the callback of the option on another thread
     * @implSpec Must block other ui actions. Must not block the thread it is called on.
     */
    Future<Option> asyncDisplayOption(String title, String text, MessageType messageType, Option[] options);
    SearchHandler getSearchHandler();

    void onFinished();

    void setSearchHandler(SearchHandler searchHandler);

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
    void start();
    default void success() {
        if (getSearchHandler() != null) {
            getSearchHandler().bisect(true);
        }
    };
    default void failure() {
        if (getSearchHandler() != null) {
            getSearchHandler().bisect(false);
        }
    };
}
