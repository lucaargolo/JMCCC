package org.to2mbn.jmccc.mcdownloader.provider.forge.fix;

import org.to2mbn.jmccc.mcdownloader.provider.forge.ForgeVersion;

import java.util.Map;

public final class BranchedVersionFix {

    private BranchedVersionFix() {
    }

    public static ForgeVersion resolve(Map<String, ForgeVersion> mapping, String requested) {
        int separator = requested.indexOf('-');
        if (separator < 0) {
            return null;
        }
        ForgeVersion candidate = mapping.get(requested.substring(0, separator));
        if (candidate != null && candidate.getBranch() != null && requested.substring(separator + 1).equals(candidate.getBranch())) {
            return candidate;
        }
        return null;
    }
}
