package org.to2mbn.jmccc.mcdownloader.provider.neoforge.fix;

public final class CalendarVersionFix {

    private CalendarVersionFix() {
    }

    public static String minecraftVersion(int major, int minor, String[] versions) {
        if (major <= 21) {
            return null;
        }

        String minecraftVersion = major + "." + minor;
        if (versions.length > 2 && !"0".equals(versions[2])) {
            minecraftVersion += "." + versions[2];
        }
        return minecraftVersion;
    }
}
