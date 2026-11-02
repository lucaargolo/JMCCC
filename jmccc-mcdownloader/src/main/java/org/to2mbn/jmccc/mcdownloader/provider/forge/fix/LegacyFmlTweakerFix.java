package org.to2mbn.jmccc.mcdownloader.provider.forge.fix;

import org.to2mbn.jmccc.internal.org.json.JSONArray;
import org.to2mbn.jmccc.internal.org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public final class LegacyFmlTweakerFix {

    private static final String[] LIBRARIES = new String[]{"net.minecraft:launchwrapper:1.3", "org.ow2.asm:asm-all:4.1", "net.sourceforge.argo:argo:3.2-small", "com.google.guava:guava:14.0-rc3", "org.bouncycastle:bcprov-jdk15on:148", "org.scala-lang:scala-library:2.10.0-custom", "lzma:lzma:0.0.1"};

    private LegacyFmlTweakerFix() {
    }

    public static void apply(JSONObject versionJson, File universal, String mavenRepositoryUrl) throws IOException {
        try (ZipFile jar = new ZipFile(universal)) {
            if (jar.getEntry("cpw/mods/fml/common/launcher/FMLTweaker.class") == null) return;
            versionJson.put("mainClass", "net.minecraft.launchwrapper.Launch");
            versionJson.put("minecraftArguments", versionJson.getString("minecraftArguments") + " --tweakClass cpw.mods.fml.common.launcher.FMLTweaker");
            JSONArray retained = new JSONArray();
            for (Object entry : versionJson.getJSONArray("libraries")) {
                String name = ((JSONObject) entry).getString("name");
                if (!name.startsWith("argo:argo:") && !name.startsWith("com.google.guava:guava:") && !name.startsWith("org.bouncycastle:bcprov-jdk15on:")) {
                    retained.put(entry);
                }
            }
            for (String name : LIBRARIES) {
                JSONObject library = new JSONObject().put("name", name);
                if (!name.startsWith("net.minecraft:") && !name.startsWith("lzma:")) {
                    library.put("url", mavenRepositoryUrl);
                }
                retained.put(library);
            }
            versionJson.put("libraries", retained);
        }
    }
}
