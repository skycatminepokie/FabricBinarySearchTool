package com.skycatdev.binarysearchtool;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;

public class Main {
    private static boolean isValidFolder(String input) {
        File inputFile = Path.of(input).toFile();
        return inputFile.exists() && inputFile.isDirectory();
    }

    public static void log(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) { // args: path (--gui) // Warning: The flag parsing is not actually flag parsing
        if (args.length > 0) {
            if (isValidFolder(args[0])) {
                if (args.length > 1) {
                    startUi(args[1].equals("--gui"), Path.of(args[0]));
                    return;
                }
                startUi(false, Path.of(args[0]));
            } else {
                System.out.println("The first argument was not a valid folder");
            }
        } else {
            SwingUtilities.invokeLater(() -> {
                String input = JOptionPane.showInputDialog(null, "Welcome! To get started, input the full path to your mods folder below.", "");
                if (isValidFolder(input)) {
                    startUi(true, Path.of(input));
                } else {
                    JOptionPane.showMessageDialog(null, "That's not a valid folder. Please try again.");
                    main(args);
                }
            });
        }
    }

    private static void startUi(boolean useGui, Path modsPath) {
        if (useGui) {
            SwingUtilities.invokeLater(() -> {
                try {
                    SearchGui gui = new SearchGui();
                    SearchHandler.createWithUi(modsPath, gui);
                    gui.setVisible(true);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("The directory should've been validated by now.", e);
                }
            });
        } else {
            try {
                SearchHandler.createWithUi(modsPath, new CliUi());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("The directory should've been validated by now.", e);
            }
        }
    }
}
