package org.to2mbn.jmccc.mcdownloader.provider.forge.fix;

import org.to2mbn.jmccc.internal.org.json.JSONObject;

public final class LegacyVersionProfileFix {

    private LegacyVersionProfileFix() {
    }

    public static void apply(JSONObject installProfile, JSONObject versionInfo) {
        JSONObject install = installProfile.optJSONObject("install");
        String parent = versionInfo.optString("inheritsFrom", install == null ? null : install.optString("minecraft", null));
        if (parent != null) {
            versionInfo.put("inheritsFrom", MinecraftVersionFix.normalize(parent));
            if (!versionInfo.has("downloads")) {
                versionInfo.put("downloads", new JSONObject().put("client", new JSONObject()));
            }
        }
    }
}
