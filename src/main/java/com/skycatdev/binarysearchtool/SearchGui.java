package com.skycatdev.binarysearchtool;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.BiConsumer;

public class SearchGui extends JFrame {
    public final JTextArea instructionsArea;
    public final JPanel bottomPanel;
    public final JButton undoButton; // TODO
    public final JButton helpButton;
    public final JProgressBar progressBar;
    public final JButton failureButton;
    public final JButton successButton;
    public final JSplitPane middlePanel;
    public final JTextPane maybeProblemPane;
    public final JTextPane notProblemPane;
    public final JPanel topPanel;
    public final JLabel folderLabel;
    public final JButton startButton;
    public JPanel mainPanel;
    public JTextField pathField;
    public @Nullable SearchHandler searchHandler = null;

    /**
     * Create the frame.
     */
    public SearchGui() {
        setTitle("Skycat's Fabric Binary Search Tool");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
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
        failureButton.addActionListener((event) -> {
            if (searchHandler != null) {
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        searchHandler.bisect(false);
                        return null;
                    }
                }.execute();
            }
        });
        bottomPanel.add(failureButton);

        successButton = new JButton("Success");
        successButton.addActionListener((event) -> {
            if (searchHandler != null) {
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        searchHandler.bisect(true);
                        return null;
                    }
                }.execute();
            }
        });
        bottomPanel.add(successButton);

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

        folderLabel = new JLabel("mods folder");
        topPanel.add(folderLabel);

        pathField = new JTextField();
        topPanel.add(pathField);
        pathField.setColumns(50);

        startButton = new JButton("Start");
        startButton.addActionListener(this::onStartButtonPressed);
        topPanel.add(startButton);

        addWindowListener(createWindowListener());

        Main.log("Initialized");
    }

    static void closeDialog(JDialog dialog, ActionEvent event) {
        dialog.setVisible(false);
        dialog.dispose();
    }

    public static void showDialog(String text, String buttonText, BiConsumer<JDialog, ActionEvent> buttonAction) {
        JDialog dialog = new JDialog();
        dialog.setLayout(new BorderLayout());
        dialog.add(new JLabel(text), BorderLayout.CENTER);
        JButton button = new JButton(buttonText);
        button.addActionListener((event) -> buttonAction.accept(dialog, event));
        dialog.add(button, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        dialog.setVisible(true);
    }

    private void onStartButtonPressed(ActionEvent event) {
        Main.log("Start button pressed");
        Path inputPath = FileSystems.getDefault().getPath(pathField.getText());
        // scuffed way of creating, bisecting, and binding but oh well
        new SwingWorker<@Nullable SearchHandler, Void>() {
            @Override
            protected @Nullable SearchHandler doInBackground() {
                return SearchHandler.createAndBind(inputPath, SearchGui.this);
            }

            @Override
            protected void done() {
                if (searchHandler == null) {
                    Main.log("Failed to make search handler");
                    startButton.setEnabled(true);
                }
            }
        }.execute();
        startButton.setEnabled(false);
    }

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

    public void updateProgress(int finished, int max) {
        Main.log("Updating progress");
        progressBar.setMaximum(max);
        progressBar.setValue(finished);
    }

    private WindowListener createWindowListener() {
        return new WindowListener() {
            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (searchHandler != null) {
                    searchHandler.onGuiClosing();
                }
                System.exit(0);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowOpened(WindowEvent e) {

            }
        };
    }
}
