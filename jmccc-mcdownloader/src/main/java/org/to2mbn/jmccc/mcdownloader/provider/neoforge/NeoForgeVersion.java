package org.to2mbn.jmccc.mcdownloader.provider.neoforge;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoForgeVersion implements Serializable {

    private static final Pattern NEOFORGE_VERSION_PATTERN = Pattern.compile("^neoforge-([\\w.\\-+]+)$");

    private static final long serialVersionUID = 1L;

    private final String neoForgeVersion;
    private final String minecraftVersion;
    private final boolean beta;

    public NeoForgeVersion(String neoForgeVersion) {
        this.neoForgeVersion = Objects.requireNonNull(neoForgeVersion);
        String[] extras = neoForgeVersion.split("\\+");
        String[] versions = neoForgeVersion.split("\\.");

        String mcVersion;


        try {
            int mcMajor = Integer.parseInt(versions[0]);
            int mcMinor = Integer.parseInt(versions[1]);

            String extra;
            if(extras.length > 1) {
                extra = "-" + extras[1];
            }else{
                extra = "";
            }

            if(mcMajor > 21) {
                mcVersion = mcMajor + "." + mcMinor + extra;
            }else{
                mcVersion = "1." + mcMajor + "." + mcMinor + extra;
            }

            if(mcVersion.endsWith(".0")) {
                mcVersion = mcVersion.substring(0, mcVersion.length() - 2);
            }
        }catch (NumberFormatException e) {
            mcVersion = versions[0] + "." + versions[1];

            if(mcVersion.startsWith("0.")) {
                mcVersion = mcVersion.substring(2);
            }
        }

        this.minecraftVersion = mcVersion;
        this.beta = neoForgeVersion.endsWith("-beta");
    }

    public static NeoForgeVersion resolve(String version) {
        Matcher matcher = NEOFORGE_VERSION_PATTERN.matcher(version);
        if (matcher.matches()) {
            String neoForgeVersion = matcher.group(1);
            return new NeoForgeVersion(neoForgeVersion);
        }

        return null;
    }

    // Getters
    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public String getNeoForgeVersion() {
        return neoForgeVersion;
    }

    public boolean isBeta() {
        return beta;
    }

    public String getVersionName() {
        return "neoforge-" + neoForgeVersion;
    }

    @Override
    public String toString() {
        return getVersionName();
    }

    @Override
    public int hashCode() {
        return neoForgeVersion.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NeoForgeVersion) {
            NeoForgeVersion another = (NeoForgeVersion) obj;
            return Objects.equals(neoForgeVersion, another.neoForgeVersion);
        }
        return false;
    }

}
