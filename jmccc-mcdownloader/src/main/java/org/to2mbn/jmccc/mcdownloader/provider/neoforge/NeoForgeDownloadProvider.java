package org.to2mbn.jmccc.mcdownloader.provider.neoforge;

import org.to2mbn.jmccc.internal.org.json.JSONObject;
import org.to2mbn.jmccc.mcdownloader.download.cache.CacheNames;
import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.tasks.MemoryDownloadTask;
import org.to2mbn.jmccc.mcdownloader.provider.*;
import org.to2mbn.jmccc.mcdownloader.provider.forge.InstallProfileProcessor;
import org.to2mbn.jmccc.mcdownloader.provider.neoforge.fix.LegacyNeoForgeFix;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.version.Library;

import java.util.List;

public class NeoForgeDownloadProvider extends AbstractMinecraftDownloadProvider {

    public static final String FORGE_GROUP_ID = "net.neoforged";
    public static final String FORGE_ARTIFACT_ID = "neoforge";
    public static final String CLASSIFIER_INSTALLER = "installer";

    private final NeoForgeDownloadSource source;

    public NeoForgeDownloadProvider() {
        this(new DefaultNeoForgeDownloadSource());
    }

    public NeoForgeDownloadProvider(NeoForgeDownloadSource source) {
        if (source == null) {
            source = new DefaultNeoForgeDownloadSource();
        }
        this.source = source;
    }

    public CombinedDownloadTask<NeoForgeVersionList> neoForgeVersionList() {
        return CombinedDownloadTask.all(
                CombinedDownloadTask.single(new MemoryDownloadTask(source.getNeoForgeMetadataUrl())
                        .andThen(new JsonDecoder())
                        .cacheable()
                        .cachePool(CacheNames.NEOFORGE_VERSION_META)),
                LegacyNeoForgeFix.versionList(source.getNeoForgeMavenRepositoryUrl())
        ).andThen(results -> {
            @SuppressWarnings("unchecked")
            List<NeoForgeVersion> legacyVersions = (List<NeoForgeVersion>) results[1];
            return NeoForgeVersionList.fromJson((JSONObject) results[0], legacyVersions);
        });
    }

    @Override
    public CombinedDownloadTask<String> gameVersionJson(final MinecraftDirectory mcdir, String version) {
        final NeoForgeVersion neoForgeInfo = NeoForgeVersion.resolve(version);

        if (neoForgeInfo != null) {
            return neoForgeVersion(neoForgeInfo)
                    .andThenDownload(forge -> installerTask(forge)
                            .andThen(installer -> LegacyNeoForgeFix.adaptInstaller(installer, forge))
                            .andThen(new InstallProfileProcessor(mcdir, FORGE_ARTIFACT_ID + "-installer.jar")));
        }

        return null;
    }

    protected CombinedDownloadTask<byte[]> installerTask(NeoForgeVersion neoForgeVersion) {
        Library lib = new Library(FORGE_GROUP_ID, neoForgeVersion.getArtifactId(), neoForgeVersion.getArtifactVersion(), CLASSIFIER_INSTALLER, "jar");
        return CombinedDownloadTask.single(
                new MemoryDownloadTask(source.getNeoForgeMavenRepositoryUrl() + lib.getPath())
                        .cacheable()
                        .cachePool(CacheNames.FORGE_INSTALLER));
    }

    private CombinedDownloadTask<NeoForgeVersion> neoForgeVersion(final NeoForgeVersion neoForgeVersion) {
        return neoForgeVersionList()
                .andThen(versionList -> {
                    NeoForgeVersion forge = versionList.get(neoForgeVersion.getNeoForgeVersion());
                    if (forge == null) {
                        throw new IllegalArgumentException("NeoForge version not found: " + neoForgeVersion);
                    }
                    return forge;
                });
    }

}
