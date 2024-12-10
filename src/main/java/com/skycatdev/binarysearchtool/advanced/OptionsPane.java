package com.skycatdev.binarysearchtool.advanced;

import com.skycatdev.binarysearchtool.SearchGui;
import com.skycatdev.binarysearchtool.SearchHandler;

import javax.swing.*;
import java.awt.*;

public class OptionsPane extends JTabbedPane {
    protected final SearchHandler searchHandler;
    public OptionsPane(SearchHandler searchHandler) {
        super();
        this.searchHandler = searchHandler;
        addTab("Mods", createModsPanel());
    }

    /**
     * Creates a panel with a label and a {@link ModsPanel} in it.
     */
    private JPanel createModsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Edit options for particular mods"), BorderLayout.NORTH);
        panel.add(new ModsPanel(searchHandler), BorderLayout.CENTER);
        return panel;
    }
}
