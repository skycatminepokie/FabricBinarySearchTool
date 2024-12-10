package com.skycatdev.binarysearchtool.advanced;

import com.skycatdev.binarysearchtool.Mod;
import com.skycatdev.binarysearchtool.SearchHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ModsPanel extends JPanel {
    protected final SearchHandler searchHandler;
    private final JComboBox<Mod> modSelectionBox;
    private int nextRow;

    public ModsPanel(SearchHandler searchHandler) {
        super();
        this.searchHandler = searchHandler;
        setLayout(new GridBagLayout());
        modSelectionBox = new JComboBox<>(searchHandler.getMods().toArray(new Mod[0]));
        GridBagConstraints modSelectionBoxConstraints = new GridBagConstraints();
        modSelectionBoxConstraints.gridx = 0;
        modSelectionBoxConstraints.gridy = 0;
        modSelectionBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> new JLabel(value.name()));
        add(modSelectionBox, modSelectionBoxConstraints);

        JButton addButton = new JButton("Force-enable");
        addButton.addActionListener(this::onForceEnablePressed);
        GridBagConstraints addButtonConstraints = new GridBagConstraints();
        addButtonConstraints.gridx = 1;
        addButtonConstraints.gridy = 0;
        add(addButton, addButtonConstraints);
        nextRow = 2;

    }

    private void onForceEnablePressed(ActionEvent actionEvent) {
        searchHandler.forceEnable(modSelectionBox.getItemAt(modSelectionBox.getSelectedIndex()));
        JOptionPane.showMessageDialog(this, "Mod force enabled!");
    }
}
