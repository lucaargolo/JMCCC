package org.to2mbn.jmccc.mcdownloader.provider.forge.fix;

import org.to2mbn.jmccc.mcdownloader.download.cache.CacheNames;
import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.tasks.FileDownloadTask;
import org.to2mbn.jmccc.mcdownloader.provider.forge.ForgeVersion;
import org.to2mbn.jmccc.util.ChecksumUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public enum LegacyForgeVersion {
    V1_0_0("b1.7.3", "1.0.0", "minecraftforge-client-1.0.0.zip", "1a141c9d6b11e9118559d9ff411db424c2b2b7b492e496fca1aad52dd5e10992"),
    V1_0_1("b1.7.3", "1.0.1", "minecraftforge-client-1.0.1.zip", "bfa6c164e335cae4e063300d255d3a51e455355975001ce55b742249770e234d"),
    V1_0_2("b1.7.3", "1.0.2", "minecraftforge-client-1.0.2.zip", "e470cdb5674e7d71863047da7edbbcd0787e356f01152c928e078cf64b7f4d96"),
    V1_0_3("b1.7.3", "1.0.3", "minecraftforge-client-1.0.3.zip", "48246421e0104551c9e7582822a89634916c38f656fb7d64560198384c779c1a"),
    V1_0_4("b1.7.3", "1.0.4", "minecraftforge-client-1.0.4.zip", "2700848c6e067d102177aaa8ade196940698596344c406714fd3bd1cfbdffa10"),
    V1_0_5("b1.7.3", "1.0.5", "minecraftforge-client-1.0.5.zip", "00e0632e62aed00835e02eef0a79c168f69098a5bc9cbdc7495b8ad67ba85246"),
    V1_0_6("b1.7.3", "1.0.6", "minecraftforge-client-1.0.6.zip", "6f69697a72ccc25ba0ac1dae34c391be67ba60395cdf32dca1170b5bb583e482"),
    V1_0_7("b1.7.3", "1.0.7", "minecraftforge-client-1.0.7-20110907.zip", "4593856b151f2ee342d0a072762d081b029021f3c503e75af1159ac787298d4e"),
    V1_1_0("b1.8.1", "1.1.0", "minecraftforge-client-1.1.0.zip", "5af0e6a029cf7ced3007f3a8318c8dab2f7f99c13f06fd231f731ce70ff279c4"),
    V1_1_1("b1.8.1", "1.1.1", "minecraftforge-client-1.1.1.zip", "4c9141d714098fcf6da2275f57ad4dd7eb083874cf0d725067290aa21a1cefa5"),
    V1_1_2("b1.8.1", "1.1.2", "minecraftforge-client-1.1.2.zip", "58348a41b62143e531677e3afc099e1cba0e4b06a76a10cc3c0c3a1c821e4b65"),
    V1_2_0("1.0.0", "1.2.0", "minecraftforge-client-1.2.0.zip", "66c5a55e49b53e98f63c6e8f0499b5cf551d335660c88a2659a32ab07640d172"),
    V1_2_1("1.0.0", "1.2.1", "minecraftforge-client-1.2.1.zip", "bdc93cd3cc3683d7147ffc63a3600680a3b541a8ec19db3fd58e91190822d1c0"),
    V1_2_2("1.0.0", "1.2.2", "minecraftforge-client-1.2.2.zip", "7aeea080469f287be57c9ebe80f2ae06bcd1bae0f1c6ac3b25e9da2ac4ff0dbf"),
    V1_2_3("1.0.0", "1.2.3", "minecraftforge-client-1.2.3.zip", "002f1a3ac276046e5c4f70c6268f4c391b38616d948909baaf8d95a79885ce01"),
    V1_2_4("1.0.0", "1.2.4", "minecraftforge-client-1.2.4.zip", "10bfe159e180a3f85c254aec76fa4ff82cb1a938cc92c3d7829889d648ab2902"),
    V1_2_5("1.0.0", "1.2.5", "minecraftforge-client-1.2.5.zip", "a4483b0121eb5ed7a4079774a33b4d8dd165472780f840539f7c6830a4cb7e38");

    private static final Map<String, LegacyForgeVersion> VERSIONS = new LinkedHashMap<>();

    static {
        for (LegacyForgeVersion version : values()) {
            VERSIONS.put(version.version.getMavenVersion(), version);
        }
    }

    private final ForgeVersion version;
    private final String fileName;
    private final String checksum;

    LegacyForgeVersion(String minecraftVersion, String forgeVersion, String fileName, String checksum) {
        this.version = ForgeVersion.from(minecraftVersion + "-" + forgeVersion);
        this.fileName = fileName;
        this.checksum = checksum;
    }

    public ForgeVersion getVersion() {
        return version;
    }

    public CombinedDownloadTask<Void> download(File target) {
        return CombinedDownloadTask.single(new FileDownloadTask("https://b2.mcarchive.net/file/mcarchive/" + checksum + "/" + fileName, target).<Void>andThen(arg -> {
            if (!ChecksumUtils.verify(target, checksum, "SHA-256")) {
                throw new IOException("Forge archive checksum mismatch: " + version.getMavenVersion());
            }
            return null;
        }).cachePool(CacheNames.FORGE_UNIVERSAL));
    }

    public static LegacyForgeVersion get(String mavenVersion) {
        return VERSIONS.get(mavenVersion);
    }

    public static Collection<LegacyForgeVersion> all() {
        return VERSIONS.values();
    }
}
