package com.skycatdev.binarysearchtool;

import com.skycatdev.binarysearchtool.advanced.OptionsPane;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class SearchGui extends JFrame implements SearchUi {
    public final JTextArea instructionsArea;
    public final JPanel bottomPanel;
    public final JButton advancedButton;
    public final JButton undoButton; // TODO
    public final JButton helpButton;
    public final JProgressBar progressBar;
    public final JButton failureButton;
    public final JButton successButton;
    public final JSplitPane middlePanel;
    public final JTextPane maybeProblemPane;
    public final JTextPane notProblemPane;
    public final JPanel topPanel;
    public final JButton startButton;
    public JPanel mainPanel;
    public @Nullable SearchHandler searchHandler = null;

    /**
     * Create the frame.
     */
    public SearchGui() {
        setTitle("Skycat's Fabric Binary Search Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 800, 500);
        mainPanel = new JPanel();
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        setContentPane(mainPanel);
        mainPanel.setLayout(new BorderLayout(0, 0));

        instructionsArea = new JTextArea();
        instructionsArea.setText("Paste the path to your mods folder above. Make sure Minecraft is closed, then click start.");
        instructionsArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        instructionsArea.setWrapStyleWord(true);
        instructionsArea.setLineWrap(true);
        instructionsArea.setSize(200, 100);
        instructionsArea.setEditable(false);
        mainPanel.add(instructionsArea, BorderLayout.EAST);

        bottomPanel = new JPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        undoButton = new JButton("Undo");
        // bottomPanel.add(undoButton);

        helpButton = new JButton("Help");
        helpButton.addActionListener((event) -> {
            JDialog dialog = new JDialog(SearchGui.this);
            dialog.setTitle("Help");
            dialog.setLayout(new BorderLayout());
            JTextArea textArea = new JTextArea("""
                    Thanks for using my Binary Search Tool! This is useful when you think there is
                    one mod that is causing problems, but you can't figure out which one from logs.
                    This will NOT work when there's more than one mod causing the problem, when
                    you're missing dependencies (including Minecraft or Java), or for non-fabric
                    things.
                    Put in the path to your mod folder and click start. Then, launch the game.
                    Click "Failure" if you have the same problem. Click "Success" if it
                    works OR you have a new problem. By repeating this a few times, you can quickly
                    find which mod is the troublemaker.
                    Fabric discord: https://discord.gg/v6v4pMv
                        - ping @skycatminepokie if this is broken
                    Github repo: https://github.com/skycatminepokie/FabricBinarySearchTool
                    Ko-Fi: https://ko-fi.com/skycatminepokie
                    Distributed under the MIT license""");
            textArea.setEditable(false);
            dialog.add(textArea, BorderLayout.CENTER);
            dialog.pack();
            dialog.setResizable(false);
            dialog.setVisible(true);
        });
        bottomPanel.add(helpButton);

        progressBar = new JProgressBar();
        progressBar.setMaximum(100);
        bottomPanel.add(progressBar);

        failureButton = new JButton("Failure");
        failureButton.addActionListener((event) -> failure());
        bottomPanel.add(failureButton);
        failureButton.setEnabled(false);

        successButton = new JButton("Success");
        successButton.addActionListener((event) -> success());
        bottomPanel.add(successButton);
        successButton.setEnabled(false);

        middlePanel = new JSplitPane();
        mainPanel.add(middlePanel, BorderLayout.CENTER);

        maybeProblemPane = new JTextPane();
        maybeProblemPane.setText("Might be the problem");
        maybeProblemPane.setEditable(false);

        JScrollPane leftSplitPane = new JScrollPane(maybeProblemPane);
        leftSplitPane.setLayout(new ScrollPaneLayout());
        leftSplitPane.createVerticalScrollBar();
        middlePanel.setLeftComponent(leftSplitPane);

        notProblemPane = new JTextPane();
        notProblemPane.setText("Not the problem");
        notProblemPane.setEditable(false);

        JScrollPane rightSplitPane = new JScrollPane(notProblemPane);
        rightSplitPane.setLayout(new ScrollPaneLayout());
        rightSplitPane.createVerticalScrollBar();
        middlePanel.setRightComponent(rightSplitPane);

        topPanel = new JPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        advancedButton = new JButton("Advanced...");
        advancedButton.addActionListener(this::openAdvancedDialog);
        topPanel.add(advancedButton);

        startButton = new JButton("Start");
        startButton.addActionListener((event) -> start());
        topPanel.add(startButton);

        setVisible(true);

        Main.log("Initialized");
    }

    @Override
    public Future<Void> asyncDisplayOption(String title, String text, MessageType messageType, Option[] options) {
        FutureTask<Void> future = new FutureTask<>(() -> (null));
        SwingUtilities.invokeLater(() -> {
            OptionDialog dialog = new OptionDialog(this, title, text, options, future);
            dialog.setVisible(true);
        });
        return future;
    }

    @Override
    public void failure() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                if (getSearchHandler() != null) {
                    getSearchHandler().bisect(false);
                }
                return null;
            }
        }.execute();
        failureButton.setEnabled(false);
        successButton.setEnabled(false);
    }

    @Override
    public @Nullable SearchHandler getSearchHandler() {
        return searchHandler;
    }

    @Override
    public void initialize(@Nullable SearchHandler searchHandler) {
        this.searchHandler = searchHandler;
    }

    @Override
    public void onBisectFinished() {
        successButton.setEnabled(true);
        failureButton.setEnabled(true);
    }

    @Override
    public void onFinished(ArrayList<Mod> problematicMods) {
        failureButton.setEnabled(false);
        successButton.setEnabled(false);
        if (problematicMods.size() == 1) {
            JOptionPane.showMessageDialog(this, "Finished! The problematic mod is " + problematicMods.getFirst().name() + ".");
        } else {
            StringBuilder message = new StringBuilder("Finished! The problem is one of these mods, which all rely on each other:");
            for (Mod problematicMod : problematicMods) {
                message.append('\n');
                message.append(problematicMod.name());
            }
            JOptionPane.showMessageDialog(this, message);
        }
    }

    private void openAdvancedDialog(ActionEvent event) {
        if (searchHandler != null) {
            JDialog dialog = new JDialog(SearchGui.this, true);
            dialog.setTitle("Advanced options");
            dialog.setLayout(new BorderLayout());
            OptionsPane advancedOptionsPane = new OptionsPane(searchHandler);
            dialog.add(advancedOptionsPane, BorderLayout.CENTER);
            dialog.pack();
            dialog.setVisible(true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        }
    }

    @Override
    public void sendInstructions(String instructions) {
        SwingUtilities.invokeLater(() -> instructionsArea.setText(instructions));
    }

    @Override
    public void start() {
        Main.log("Requested start searching");
        if (getSearchHandler() != null) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    if (getSearchHandler() != null) {
                        getSearchHandler().bisect(true);
                    }
                    return null;
                }
            }.execute();
            startButton.setEnabled(false);
            advancedButton.setEnabled(false);
        }
    }

    @Override
    public void success() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                if (getSearchHandler() != null) {
                    getSearchHandler().bisect(true);
                }
                return null;
            }
        }.execute();
        failureButton.setEnabled(false);
        successButton.setEnabled(false);
    }

    @Override
    public void updateLists(ArrayList<Mod> candidateMods, ArrayList<Mod> workingMods) {
        Main.log("Updating lists");
        StringBuilder maybeProblem = new StringBuilder("Might be the problem:\n");
        for (Mod candidate : candidateMods) {
            maybeProblem.append(candidate.name());
            maybeProblem.append('\n');
        }
        maybeProblemPane.setText(maybeProblem.toString());
        StringBuilder notProblem = new StringBuilder("Not the problem:\n");
        for (Mod candidate : workingMods) {
            notProblem.append(candidate.name());
            notProblem.append('\n');
        }
        notProblemPane.setText(notProblem.toString());
    }

    @Override
    public void updateProgress(int finished, int max) {
        Main.log("Updating progress");
        progressBar.setMaximum(max);
        progressBar.setValue(finished);
    }
}
