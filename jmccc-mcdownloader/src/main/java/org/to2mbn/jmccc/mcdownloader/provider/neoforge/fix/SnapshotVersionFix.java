package org.to2mbn.jmccc.mcdownloader.provider.neoforge.fix;

public final class SnapshotVersionFix {

    private SnapshotVersionFix() {
    }

    public static String minecraftVersion(String[] versions) {
        String minecraftVersion = versions[0] + "." + versions[1];
        return minecraftVersion.startsWith("0.") ? minecraftVersion.substring(2) : minecraftVersion;
    }

    public static String suffix(String neoForgeVersion, int separator) {
        return separator < 0 ? "" : "-" + neoForgeVersion.substring(separator + 1);
    }
}
