package org.to2mbn.jmccc.mcdownloader.provider.forge.fix;

import java.util.zip.ZipEntry;

public final class JarEntryFix {

    private JarEntryFix() {
    }

    public static ZipEntry copy(ZipEntry original) {
        ZipEntry entry = new ZipEntry(original);
        entry.setCompressedSize(-1);
        return entry;
    }
}
