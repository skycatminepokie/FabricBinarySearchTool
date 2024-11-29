package com.skycatdev.binarysearchtool;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class SwingWorkerCompletionWaiter implements PropertyChangeListener {
    private JDialog dialog;
    private Runnable onFinished;

    public SwingWorkerCompletionWaiter(JDialog dialog, Runnable onFinished) {
        this.dialog = dialog;
        this.onFinished = onFinished;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if ("state".equals(event.getPropertyName()) &&
            SwingWorker.StateValue.DONE == event.getNewValue()) {
            dialog.setVisible(false);
            dialog.dispose();
            onFinished.run();
        }
    }
}
