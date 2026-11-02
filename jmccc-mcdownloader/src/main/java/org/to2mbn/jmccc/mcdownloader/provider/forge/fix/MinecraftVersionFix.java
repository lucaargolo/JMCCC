package org.to2mbn.jmccc.mcdownloader.provider.forge.fix;

public final class MinecraftVersionFix {

    private MinecraftVersionFix() {
    }

    public static String normalize(String version) {
        if ("1.0.0".equals(version)) return "1.0";
        if ("1.4.0".equals(version)) return "1.4";
        if ("1.7.10_pre4".equals(version)) return "1.7.10-pre4";
        return version;
    }
}
