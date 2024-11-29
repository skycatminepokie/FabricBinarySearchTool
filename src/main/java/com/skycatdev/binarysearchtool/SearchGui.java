package com.skycatdev.binarysearchtool;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;

public class SearchGui extends JFrame {
    public final JTextArea instructionsArea;
    public final JPanel bottomPanel;
    public final JButton undoButton; // TODO
    public final JButton helpButton; // TODO
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
        bottomPanel.add(undoButton);

        helpButton = new JButton("Help");
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
        middlePanel.setLeftComponent(maybeProblemPane);

        notProblemPane = new JTextPane();
        notProblemPane.setText("Not the problem");
        notProblemPane.setEditable(false);
        middlePanel.setRightComponent(notProblemPane);

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
    }

    private void onStartButtonPressed(ActionEvent event) {
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
                    startButton.setEnabled(true);
                }
            }
        }.execute();
        startButton.setEnabled(false);
    }

    public void updateLists(ArrayList<Mod> candidateMods, ArrayList<Mod> workingMods) {
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
        progressBar.setMaximum(max);
        progressBar.setValue(finished);
    }

}
