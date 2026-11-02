package org.to2mbn.jmccc.mcdownloader.provider.neoforge;

import org.to2mbn.jmccc.mcdownloader.provider.neoforge.fix.CalendarVersionFix;
import org.to2mbn.jmccc.mcdownloader.provider.neoforge.fix.LegacyNeoForgeFix;
import org.to2mbn.jmccc.mcdownloader.provider.neoforge.fix.SnapshotVersionFix;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoForgeVersion implements Serializable {

    private static final Pattern NEOFORGE_VERSION_PATTERN = Pattern.compile("^neoforge-([\\w.\\-+]+)$");
    private static final String MODERN_ARTIFACT_ID = "neoforge";

    private static final long serialVersionUID = 1L;

    private final String neoForgeVersion;
    private final String minecraftVersion;
    private final boolean beta;
    private final boolean legacy;

    public NeoForgeVersion(String neoForgeVersion) {
        this(neoForgeVersion, LegacyNeoForgeFix.normalize(Objects.requireNonNull(neoForgeVersion)));
    }

    private NeoForgeVersion(String neoForgeVersion, String legacyVersion) {
        this.legacy = legacyVersion != null;
        this.neoForgeVersion = legacy ? legacyVersion : neoForgeVersion;
        this.minecraftVersion = legacy ? LegacyNeoForgeFix.MINECRAFT_VERSION : resolveMinecraftVersion(neoForgeVersion);
        this.beta = this.neoForgeVersion.endsWith("-beta");
    }

    public static NeoForgeVersion legacy(String neoForgeVersion) {
        return new NeoForgeVersion(neoForgeVersion, neoForgeVersion);
    }

    public static NeoForgeVersion resolve(String version) {
        Matcher matcher = NEOFORGE_VERSION_PATTERN.matcher(version);
        if (matcher.matches()) {
            String neoForgeVersion = matcher.group(1);
            return new NeoForgeVersion(neoForgeVersion);
        }

        return LegacyNeoForgeFix.resolve(version);
    }

    private static String resolveMinecraftVersion(String neoForgeVersion) {
        int metadata = neoForgeVersion.indexOf('+');
        String[] versions = (metadata < 0 ? neoForgeVersion : neoForgeVersion.substring(0, metadata)).split("\\.");
        try {
            int major = Integer.parseInt(versions[0]);
            int minor = Integer.parseInt(versions[1]);
            String minecraftVersion = CalendarVersionFix.minecraftVersion(major, minor, versions);
            if (minecraftVersion == null) {
                minecraftVersion = "1." + major + (minor == 0 ? "" : "." + minor);
            }
            return minecraftVersion + SnapshotVersionFix.suffix(neoForgeVersion, metadata);
        } catch (NumberFormatException e) {
            return SnapshotVersionFix.minecraftVersion(versions);
        }
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

    public boolean isLegacy() {
        return legacy;
    }

    public String getArtifactId() {
        return legacy ? LegacyNeoForgeFix.ARTIFACT_ID : MODERN_ARTIFACT_ID;
    }

    public String getArtifactVersion() {
        return legacy ? minecraftVersion + "-" + neoForgeVersion : neoForgeVersion;
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
