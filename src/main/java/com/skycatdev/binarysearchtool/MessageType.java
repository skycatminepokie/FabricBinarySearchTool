package com.skycatdev.binarysearchtool;

import javax.swing.*;

public enum MessageType {
    NONE(JOptionPane.PLAIN_MESSAGE),
    INFO(JOptionPane.INFORMATION_MESSAGE),
    WARNING(JOptionPane.WARNING_MESSAGE),
    ERROR(JOptionPane.ERROR_MESSAGE);

    public final int jOptionPaneMessageType;

    MessageType(int jOptionPaneMessageType) {
        this.jOptionPaneMessageType = jOptionPaneMessageType;
    }
}
