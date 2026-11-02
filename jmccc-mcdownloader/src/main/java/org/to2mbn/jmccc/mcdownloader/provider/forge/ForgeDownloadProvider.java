package org.to2mbn.jmccc.mcdownloader.provider.forge;

import org.to2mbn.jmccc.internal.org.json.JSONException;
import org.to2mbn.jmccc.internal.org.json.JSONObject;
import org.to2mbn.jmccc.mcdownloader.download.cache.CacheNames;
import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.tasks.FileDownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.tasks.MemoryDownloadTask;
import org.to2mbn.jmccc.mcdownloader.provider.*;
import org.to2mbn.jmccc.mcdownloader.provider.forge.fix.*;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.util.IOUtils;
import org.to2mbn.jmccc.version.Library;
import org.to2mbn.jmccc.version.Version;
import org.to2mbn.jmccc.version.parsing.Versions;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

public class ForgeDownloadProvider extends AbstractMinecraftDownloadProvider implements ExtendedDownloadProvider {

    private static final String[] UNIVERSAL_TYPES = new String[]{"jar", "zip"};

    public static final String FORGE_GROUP_ID = "net.minecraftforge";
    public static final String FORGE_ARTIFACT_ID = "forge";
    public static final String FORGE_OLD_ARTIFACT_ID = "minecraftforge";
    public static final String CLASSIFIER_INSTALLER = "installer";
    public static final String CLASSIFIER_UNIVERSAL = "universal";

    private final ForgeDownloadSource source;

    private MinecraftDownloadProvider upstreamProvider;

    public ForgeDownloadProvider() {
        this(new DefaultForgeDownloadSource());
    }

    public ForgeDownloadProvider(ForgeDownloadSource source) {
        if (source == null) {
            source = new DefaultForgeDownloadSource();
        }
        this.source = source;
    }

    public CombinedDownloadTask<ForgeVersionList> forgeVersionList() {
        return CombinedDownloadTask.all(
                new MemoryDownloadTask(source.getForgeMetadataUrl())
                        .andThen(new JsonDecoder())
                        .cacheable()
                        .cachePool(CacheNames.FORGE_VERSION_META),
                new MemoryDownloadTask(source.getForgePromotionUrl())
                        .andThen(new JsonDecoder())
                        .cacheable()
                        .cachePool(CacheNames.FORGE_VERSION_PROMO)
        ).andThen(it -> ForgeVersionList.fromJson((JSONObject) it[0], (JSONObject) it[1]));
    }

    @Override
    public CombinedDownloadTask<String> gameVersionJson(final MinecraftDirectory mcdir, String version) {
        final ResolvedForgeVersion forgeInfo = ResolvedForgeVersion.resolve(version);
        if (forgeInfo != null) {
            return forgeVersion(forgeInfo.getForgeVersion())
                    .andThenDownload(forge -> DeobfRealmsFix.prepare(mcdir, forge.getMinecraftVersion(), upstreamProvider)
                            .andThenDownload(parent -> {
                                CombinedDownloadTask<Callable<String>> installer = installerTask(forge.getMavenVersion())
                                        .andThen(bytes -> () -> new InstallProfileProcessor(mcdir, FORGE_ARTIFACT_ID + "-installer.jar").process(bytes));
                                File universal = mcdir.getLibrary(new Library(FORGE_GROUP_ID, FORGE_OLD_ARTIFACT_ID, forge.getForgeVersion()));
                                CombinedDownloadTask<Callable<String>> legacy = universalTask(forge.getMavenVersion(), universal)
                                        .andThen(arg -> () -> new VersionJsonInstaller(mcdir).process(createForgeVersionJson(mcdir, forge)));
                                return CombinedDownloadTask.any(installer, legacy).andThen(Callable::call);
                            }));
        }
        return null;
    }

    @Override
    public CombinedDownloadTask<Void> library(final MinecraftDirectory mcdir, final Library library) {
        if (FORGE_GROUP_ID.equals(library.getGroupId())) {

            if (FORGE_ARTIFACT_ID.equals(library.getArtifactId())) {
                if (library.getClassifier() == null || CLASSIFIER_UNIVERSAL.equals(library.getClassifier())) {
                    return universalTask(library.getVersion(), mcdir.getLibrary(library));
                }

            } else if (FORGE_OLD_ARTIFACT_ID.equals(library.getArtifactId())) {
                return forgeVersion(library.getVersion())
                        .andThenDownload(version -> universalTask(version.getMavenVersion(), mcdir.getLibrary(library)));
            }
        }
        return null;
    }

    @Override
    public CombinedDownloadTask<Void> gameJar(final MinecraftDirectory mcdir, final Version version) {
        ResolvedForgeVersion resolved = ResolvedForgeVersion.resolve(version.getVersion());
        if (resolved == null) resolved = ResolvedForgeVersion.resolve(version.getRoot());
        final ResolvedForgeVersion forgeInfo = resolved;
        if (forgeInfo == null) {
            return null;
        }

        boolean mergeJar = true;
        for (Library library : version.getLibraries()) {
            if (library.getGroupId().equals(FORGE_GROUP_ID)) {
                mergeJar = false;
                break;
            }
        }

        // downloads the super version
        CombinedDownloadTask<Version> baseTask;
        if (forgeInfo.getMinecraftVersion() == null) {
            baseTask = forgeVersion(forgeInfo.getForgeVersion())
                    .andThenDownload(forge -> downloadSuperVersion(mcdir, forge.getMinecraftVersion()));
        } else {
            baseTask = downloadSuperVersion(mcdir, forgeInfo.getMinecraftVersion());
        }

        final File targetJar = mcdir.getVersionJar(version.getVersion());

        if (mergeJar) {
            // downloads the universal
            // copy its superversion's jar
            // remove META-INF
            // copy universal into the jar
            final File universalFile = mcdir.getLibrary(new Library(FORGE_GROUP_ID, FORGE_OLD_ARTIFACT_ID, forgeInfo.getForgeVersion()));
            return baseTask
                    .andThenDownload(superVersion -> forgeVersion(forgeInfo.getForgeVersion())
                            .andThenDownload(forge -> universalTask(forge.getMavenVersion(), universalFile)
                                    .andThenDownload(arg -> CombinedDownloadTask.all(LegacyFmlLibrariesFix.download(mcdir, universalFile), LegacyModLoaderFix.download(mcdir, forge.getMinecraftVersion(), universalFile)))
                                    .andThen(results -> {
                                        LegacyJarFix.merge(mcdir.getVersionJar(superVersion), universalFile, targetJar, (File[]) results[1]);
                                        return null;
                                    })));
        } else {
            // copy its superversion's jar
            // remove META-INF
            return baseTask.andThen(superVersion -> {
                LegacyJarFix.purgeMetaInf(mcdir.getVersionJar(superVersion), targetJar);
                return null;
            });
        }
    }

    @Override
    public void setUpstreamProvider(MinecraftDownloadProvider upstreamProvider) {
        this.upstreamProvider = upstreamProvider;
    }

    protected CombinedDownloadTask<byte[]> installerTask(String m2Version) {
        Library lib = new Library(FORGE_GROUP_ID, FORGE_ARTIFACT_ID, m2Version, CLASSIFIER_INSTALLER, "jar");
        return CombinedDownloadTask.single(
                new MemoryDownloadTask(source.getForgeMavenRepositoryUrl() + lib.getPath())
                        .cacheable()
                        .cachePool(CacheNames.FORGE_INSTALLER));
    }

    protected CombinedDownloadTask<Void> universalTask(String m2Version, File target) {
        LegacyForgeVersion legacy = LegacyForgeVersion.get(m2Version);
        if (legacy != null) return legacy.download(target);

        @SuppressWarnings("unchecked")
        CombinedDownloadTask<Void>[] tasks = new CombinedDownloadTask[UNIVERSAL_TYPES.length + 2];
        tasks[0] = installerTask(m2Version).andThen(new UniversalDecompressor(target, m2Version));
        int index = 1;
        for (String type : UNIVERSAL_TYPES) {
            Library lib = new Library(FORGE_GROUP_ID, FORGE_ARTIFACT_ID, m2Version, CLASSIFIER_UNIVERSAL, type);
            tasks[index++] = CombinedDownloadTask.single(new FileDownloadTask(source.getForgeMavenRepositoryUrl() + lib.getPath(), target).cachePool(CacheNames.FORGE_UNIVERSAL));
        }
        tasks[index] = ClientArchiveFix.download(source, m2Version, target);
        return CombinedDownloadTask.any(tasks);
    }

    protected JSONObject createForgeVersionJson(MinecraftDirectory mcdir, ForgeVersion forgeVersion) throws IOException, JSONException {
        JSONObject versionjson = IOUtils.toJson(mcdir.getVersionJson(forgeVersion.getMinecraftVersion()));
        versionjson.remove("downloads");
        versionjson.put("id", forgeVersion.getVersionName());
        File universal = mcdir.getLibrary(new Library(FORGE_GROUP_ID, FORGE_OLD_ARTIFACT_ID, forgeVersion.getForgeVersion()));
        LegacyFmlTweakerFix.apply(versionjson, universal, source.getForgeMavenRepositoryUrl());
        return versionjson;
    }

    private CombinedDownloadTask<ForgeVersion> forgeVersion(final String forgeVersion) {
        return forgeVersionList()
                .andThen(versionList -> {
                    ForgeVersion forge = versionList.get(forgeVersion);
                    if (forge == null) {
                        throw new IllegalArgumentException("Forge version not found: " + forgeVersion);
                    }
                    return forge;
                });
    }

    private CombinedDownloadTask<Version> downloadSuperVersion(final MinecraftDirectory mcdir, String version) {
        return upstreamProvider.gameVersionJson(mcdir, version)
                .andThenDownload(resolvedMcversion -> {
                    final Version superversion = Versions.resolveVersion(mcdir, resolvedMcversion);
                    return upstreamProvider.gameJar(mcdir, superversion).andThenReturn(superversion);
                });
    }
}
