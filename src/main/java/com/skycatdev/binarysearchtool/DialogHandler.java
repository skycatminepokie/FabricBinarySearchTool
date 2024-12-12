package com.skycatdev.binarysearchtool;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.FutureTask;

/**
 * Runs every task on a new thread, waiting for each task to be done before starting the next.
 */
public class DialogHandler {
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
