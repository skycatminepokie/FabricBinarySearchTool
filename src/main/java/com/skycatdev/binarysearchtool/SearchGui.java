package com.skycatdev.binarysearchtool;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import javax.swing.JButton;
import java.awt.FlowLayout;
import javax.swing.JSplitPane;
import javax.swing.JProgressBar;
import javax.swing.JTextPane;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class SearchGui extends JFrame {
    private final JTextPane instructionsPane;
    private final JPanel bottomPanel;
    private final JButton undoButton;
    private final JButton helpButton;
    private final JProgressBar progressBar;
    private final JButton failureButton;
    private final JButton successButton;
    private final JSplitPane middlePanel;
    private final JTextPane maybeProblemPane;
    private final JTextPane notProblemPane;
    private final JPanel topPanel;
    private final JLabel folderLabel;
    private final JButton startButton;
    private JPanel mainPanel;
    private JTextField folderField;

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

        instructionsPane = new JTextPane();
        instructionsPane.setText("Instructions");
        mainPanel.add(instructionsPane, BorderLayout.EAST);

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
        bottomPanel.add(failureButton);

        successButton = new JButton("Success");
        bottomPanel.add(successButton);

        middlePanel = new JSplitPane();
        mainPanel.add(middlePanel, BorderLayout.CENTER);

        maybeProblemPane = new JTextPane();
        maybeProblemPane.setText("Might be the problem");
        middlePanel.setLeftComponent(maybeProblemPane);

        notProblemPane = new JTextPane();
        notProblemPane.setText("Not the problem");
        middlePanel.setRightComponent(notProblemPane);

        topPanel = new JPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        folderLabel = new JLabel(".minecraft Folder");
        topPanel.add(folderLabel);

        folderField = new JTextField();
        topPanel.add(folderField);
        folderField.setColumns(50);

        startButton = new JButton("Start");
        topPanel.add(startButton);
    }

}
