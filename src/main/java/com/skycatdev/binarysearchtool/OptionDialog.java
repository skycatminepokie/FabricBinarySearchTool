package com.skycatdev.binarysearchtool;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.FutureTask;

public class OptionDialog extends JDialog {
    @Nullable Option chosenOption = null;
    public OptionDialog(Frame owner, String title, String text, Option[] options, FutureTask<Void> future) {
        super(owner, true);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setTitle(title);

        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        this.add(textArea, BorderLayout.CENTER);

        JButton[] buttons = new JButton[options.length];
        for (int i = 0; i < options.length; i++) {
            buttons[i] = makeOptionButton(options[i], future);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout());
        for (JButton button : buttons) {
            buttonPanel.add(button);
        }
        add(buttonPanel, BorderLayout.SOUTH);
        pack();
    }

    private @NotNull JButton makeOptionButton(Option option, FutureTask<Void> future) {
        JButton button = new JButton(option.name());
        button.addActionListener((event) -> {
            if (option.callback() != null) {
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        future.run();
                        option.callback().run();
                        return null;
                    }
                }.execute();
                setVisible(false);
                dispose();
            }
        });
        return button;
    }
}
