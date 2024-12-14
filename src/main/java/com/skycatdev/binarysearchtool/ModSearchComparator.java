package com.skycatdev.binarysearchtool;

import java.util.Comparator;

public class ModSearchComparator implements Comparator<Mod> {
    @Override
    public int compare(Mod o1, Mod o2) {
        if (o1.dependencies().size() > o2.dependencies().size()) { // Most dependencies first
            return -1;
        }
        return o1.mainId().compareTo(o2.mainId()); // Mods ids shouldn't be the same. If they are, oh well.
    }
}
