package com.skycatdev.binarysearchtool;

import java.util.Comparator;

public class ModSearchComparator implements Comparator<Mod> {
    @Override
    public int compare(Mod o1, Mod o2) {
        if (o1.equals(o2)) return 0;
        if (o1.dependencies().size() > o2.dependencies().size()) { // Most dependencies first
            return -1;
        }
        int mainIdCompare = o1.mainId().compareTo(o2.mainId());
        if (mainIdCompare != 0) {
            return mainIdCompare;
        }
        int nameCompare = o1.name().compareTo(o2.name());
        if (nameCompare != 0) {
            return nameCompare;
        }
        int fileCompare = o1.filename().compareTo(o2.filename());
        if (fileCompare != 0) {
            return fileCompare;
        }
        throw new AssertionError("We compared main ids, name, filename, ids, and deps, and they are all equal, but they weren't the same??? I give up.");
    }
}
