package com.skycatdev.binarysearchtool;

import java.awt.*;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            SearchGui gui = new SearchGui();
            gui.setVisible(true);
            if (args.length > 0) {
                gui.startSearching(Path.of(args[0]));
            }
        });
    }

    public static void log(String message) {
        System.out.println(message);
    }
}
