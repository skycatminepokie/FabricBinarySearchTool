package com.skycatdev.binarysearchtool;

import javax.swing.*;
import java.io.File;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        boolean enableGui = true; // TODO: Accept args
        if (args.length > 0) {
            String pathString = args[0]; // TODO: Don't hardcode location
            if (isValidFolder(pathString)) {
                startUi(enableGui, Path.of(pathString));
            } else {
                log("Invalid path string");
                if (enableGui) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Command-line specified path is invalid.", "Error", JOptionPane.ERROR_MESSAGE));
                } else {
                    System.out.println("Command-line specified path is invalid.");
                }
            }
        } else {
            SwingUtilities.invokeLater(() -> {
                String input = JOptionPane.showInputDialog(null, "Welcome! To get started, input the full path to your mods folder below.", "");
                if (isValidFolder(input)) {
                    startUi(enableGui, Path.of(input));
                } else {
                    JOptionPane.showMessageDialog(null, "That's not a valid folder. Please try again.");
                    main(args);
                }
            });
        }
    }

    private static void startUi(boolean enableGui, Path modsPath) {
        if (enableGui) {
            SwingUtilities.invokeLater(() -> {
                try {
                    SearchGui gui = new SearchGui();
                    SearchHandler.createWithUi(modsPath, gui);
                    gui.setVisible(true);
                } catch (NotDirectoryException | IllegalArgumentException e) {
                    throw new RuntimeException("The directory should've been validated by now.", e);
                }
            });
        } else {
            try {
                SearchHandler.createWithUi(modsPath, new CliUi());
            } catch (NotDirectoryException | IllegalArgumentException e) {
                throw new RuntimeException("The directory should've been validated by now.", e);
            }
        }
    }

    private static boolean isValidFolder(String input) {
        File inputFile = Path.of(input).toFile();
        return inputFile.exists() && inputFile.isDirectory();
    }

    public static void log(String message) {
        System.out.println(message);
    }
}
