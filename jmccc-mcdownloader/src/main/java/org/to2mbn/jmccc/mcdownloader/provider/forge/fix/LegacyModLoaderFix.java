package org.to2mbn.jmccc.mcdownloader.provider.forge.fix;

import org.to2mbn.jmccc.mcdownloader.download.cache.CacheNames;
import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.tasks.FileDownloadTask;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.util.ChecksumUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

public final class LegacyModLoaderFix {

    private static final LegacyLoader[] LOADERS = LegacyLoader.values();

    private LegacyModLoaderFix() {
    }

    public static CombinedDownloadTask<File[]> download(MinecraftDirectory directory, String minecraftVersion, File universal) throws Exception {
        List<CombinedDownloadTask<?>> tasks = new ArrayList<>();
        List<File> files = new ArrayList<>();
        try (ZipFile jar = new ZipFile(universal)) {
            if (jar.getEntry("ModLoader.class") == null) {
                for (LegacyLoader loader : LOADERS) {
                    if (!loader.minecraftVersion.equals(minecraftVersion)) continue;
                    File target = directory.get("libraries/forge-legacy/" + loader.hash + ".zip").toFile();
                    files.add(target);
                    if (ChecksumUtils.verify(target, loader.hash, "SHA-256")) continue;
                    String url = "https://b2.mcarchive.net/file/mcarchive/" + loader.hash + "/" + loader.fileName;
                    tasks.add(CombinedDownloadTask.single(new FileDownloadTask(url, target).andThen(arg -> {
                        if (!ChecksumUtils.verify(target, loader.hash, "SHA-256")) {
                            throw new IOException("Forge prerequisite checksum mismatch: " + loader.fileName);
                        }
                        return null;
                    }).cachePool(CacheNames.LIBRARY)));
                }
            }
        }
        return CombinedDownloadTask.multiple(tasks.toArray(new CombinedDownloadTask<?>[0])).andThenReturn(files.toArray(new File[0]));
    }

    private enum LegacyLoader {
        MODLOADER_BETA_1_7_3("b1.7.3", "ModLoader%20B1.7.3.zip", "78bc1107a2ae78334d1086c7f372601c141b53345f23ce73931ef318df5cf83e"),
        MODLOADER_MP_BETA_1_7_3("b1.7.3", "ModLoaderMp%201.7.3%20Unofficial%20v2.zip", "69dc20ce6cb578e0101471ea9a4e270a45f7f771cc46fcbab993bef7e9da4e23"),
        MODLOADER_BETA_1_8_1("b1.8.1", "ModLoader%20B1.8.1.zip", "4135de0b0fddf6f9b39761a5261b82dae278b311237ec1cd936911b0b133919e"),
        MODLOADER_MP_BETA_1_8_1("b1.8.1", "ModLoaderMp%201.8.1.zip", "a08a5844da4ef7bf751a19359eb8cfd99dbaed846ecc4cd50386cf1668869377"),
        MODLOADER_1_0("1.0", "ModLoader%201.0.0.zip", "0abd012bcfd536522d50ac642080d6164cd6cdc22629386a0e8e1fafa2e7cd99"),
        MODLOADER_MP_1_0("1.0", "ModLoaderMp%201.0.0.zip", "9183a9fde186ca779c68d43f545e487e83ffe987cc7fea99d4aa17f253fd2cb9"),
        MODLOADER_1_1("1.1", "ModLoader%201.1.zip", "b56d925adc210773e4b2390f8189e16456910da4bcd4276b492bb382ea04f079"),
        MODLOADER_MP_1_1("1.1", "ModLoaderMp%201.1%20v2.1.zip", "40028c72b76d181189eaaa66d04a2014d89975e0e8a843379d3a19eb5d96213e"),
        MODLOADER_1_2_3("1.2.3", "ModLoader%201.2.3.zip", "3d3fbbd5cefe409c9c7328a57c4cd854e72d7a6dce6e829e2120c0ad5e1a5f63"),
        MODLOADER_MP_1_2_3("1.2.3", "ModLoaderMP%201.2.3%20v3.zip", "b6bc6d0a4fa21361429f39c2847fa47420e60d54263f9a4abc2fe74dea6814c8"),
        MODLOADER_1_2_4("1.2.4", "ModLoader%201.2.4.zip", "0f9bb79149f95061bee539a3120d820043e84ed8747e9ca98756fc34e40b7d7d"),
        MODLOADER_MP_1_2_4("1.2.4", "ModLoaderMP%201.2.4%20v1.zip", "30fc0c1b7dd9693c19fb419353bf9b0533d4d3248772ca93b6f5b78bbc70e5d6"),
        MODLOADER_1_2_5("1.2.5", "ModLoader%201.2.5.zip", "219370a86a15bfef8ff91f51fdd151e99391b771759183b19f72197452a28b79");

        private final String minecraftVersion;
        private final String fileName;
        private final String hash;

        LegacyLoader(String minecraftVersion, String fileName, String hash) {
            this.minecraftVersion = minecraftVersion;
            this.fileName = fileName;
            this.hash = hash;
        }
    }
}
