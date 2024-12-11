package com.skycatdev.binarysearchtool;

import com.skycatdev.binarysearchtool.advanced.OptionsPane;
import org.jetbrains.annotations.NotNull;
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
        pathField.setColumns(45);

        advancedButton = new JButton("Advanced...");
        advancedButton.addActionListener(this::openAdvancedDialog);
        topPanel.add(advancedButton);

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

    private void openAdvancedDialog(ActionEvent event) {
        JDialog discoveringDialog = makeDiscoveryDialog();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                Path inputPath = FileSystems.getDefault().getPath(pathField.getText());
                SearchHandler.createAndBind(inputPath, SearchGui.this);
                return null;
            }

            @Override
            protected void done() {
                discoveringDialog.dispose();
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
        }.execute();
        discoveringDialog.setVisible(true);
    }

    private @NotNull JDialog makeDiscoveryDialog() {
        JDialog discoveringDialog = new JDialog(this, true);
        discoveringDialog.setTitle("Discovering mods");
        discoveringDialog.setLayout(new BorderLayout());
        JTextArea textArea = new JTextArea("Discovering mods...\nPlease wait...");
        textArea.setEditable(false);
        discoveringDialog.add(textArea, BorderLayout.CENTER);
        discoveringDialog.pack();
        discoveringDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        discoveringDialog.setResizable(false);
        return discoveringDialog;
    }


    private void onStartButtonPressed(ActionEvent event) {
        startSearching(FileSystems.getDefault().getPath(pathField.getText()));
    }

    @Override
    public SearchHandler getSearchHandler() {
        return searchHandler;
    }

    @Override
    public void setSearchHandler(@Nullable SearchHandler searchHandler) {
        this.searchHandler = searchHandler;
    }

    public void startSearching(Path modsPath) {
        Main.log("Requested start searching");
        // scuffed way of creating, bisecting, and binding but oh well
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                if (searchHandler == null) {
                    SearchHandler.createAndBind(modsPath, SearchGui.this);
                }
                if (searchHandler != null) { // Yes, this may change due to the other block
                    searchHandler.bisect(true);
                }
                return null;
            }

            @Override
            protected void done() {
                if (searchHandler == null) {
                    Main.log("Failed to make search handler");
                    startButton.setEnabled(true);
                    advancedButton.setEnabled(true);
                }
            }
        }.execute();
        startButton.setEnabled(false);
        advancedButton.setEnabled(false);
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
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            searchHandler.onUiClosing();
                            return null;
                        }
                    }.execute();
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
