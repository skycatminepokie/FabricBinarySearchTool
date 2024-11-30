package com.skycatdev.binarysearchtool;

import java.awt.*;

public class Main {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            SearchGui gui = new SearchGui();
            gui.setVisible(true);
        });
    }

    public static void log(String message) {
        System.out.println(message);
    }
}
