package org.to2mbn.jmccc.mcdownloader.provider.forge.fix;

import org.to2mbn.jmccc.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class LegacyJarFix {

    private LegacyJarFix() {
    }

    public static void merge(File parent, File universal, File target, File... prerequisites) throws IOException {
        FileUtils.prepareWrite(target);
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(parent.toPath()));
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target.toPath()))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            int read;
            Set<String> installedEntries = new HashSet<>();
            for (int i = prerequisites.length; i >= 0; i--) {
                File overlay = i == prerequisites.length ? universal : prerequisites[i];
                try (ZipInputStream overlayIn = new ZipInputStream(Files.newInputStream(overlay.toPath()))) {
                    while ((entry = overlayIn.getNextEntry()) != null) {
                        if (installedEntries.add(entry.getName())) {
                            out.putNextEntry(JarEntryFix.copy(entry));
                            while ((read = overlayIn.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                            out.closeEntry();
                        }
                        overlayIn.closeEntry();
                    }
                }
            }
            while ((entry = in.getNextEntry()) != null) {
                if (isNotMetaInfEntry(entry) && !installedEntries.contains(entry.getName())) {
                    out.putNextEntry(JarEntryFix.copy(entry));
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.closeEntry();
                }
                in.closeEntry();
            }
        }
    }

    public static void purgeMetaInf(File source, File target) throws IOException {
        FileUtils.prepareWrite(target);
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(source.toPath()));
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target.toPath()))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            int read;
            while ((entry = in.getNextEntry()) != null) {
                if (isNotMetaInfEntry(entry)) {
                    out.putNextEntry(JarEntryFix.copy(entry));
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.closeEntry();
                }
                in.closeEntry();
            }
        }
    }

    private static boolean isNotMetaInfEntry(ZipEntry entry) {
        return !entry.getName().startsWith("META-INF/");
    }
}
