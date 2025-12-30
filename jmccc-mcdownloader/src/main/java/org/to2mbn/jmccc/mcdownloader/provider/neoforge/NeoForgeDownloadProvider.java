package org.to2mbn.jmccc.mcdownloader.provider.neoforge;

import org.to2mbn.jmccc.internal.org.json.JSONException;
import org.to2mbn.jmccc.internal.org.json.JSONObject;
import org.to2mbn.jmccc.mcdownloader.download.cache.CacheNames;
import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.tasks.MemoryDownloadTask;
import org.to2mbn.jmccc.mcdownloader.provider.*;
import org.to2mbn.jmccc.mcdownloader.provider.forge.*;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.util.FileUtils;
import org.to2mbn.jmccc.util.IOUtils;
import org.to2mbn.jmccc.version.Library;
import org.to2mbn.jmccc.version.Version;
import org.to2mbn.jmccc.version.parsing.Versions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class NeoForgeDownloadProvider extends AbstractMinecraftDownloadProvider implements ExtendedDownloadProvider {

    public static final String FORGE_GROUP_ID = "net.neoforged";
    public static final String FORGE_ARTIFACT_ID = "neoforge";
    public static final String CLASSIFIER_INSTALLER = "installer";
    public static final String MINECRAFT_MAINCLASS = "net.minecraft.client.Minecraft";

    private final NeoForgeDownloadSource source;

    private MinecraftDownloadProvider upstreamProvider;

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
        return CombinedDownloadTask.single(new MemoryDownloadTask(source.getNeoForgeMetadataUrl())
                        .andThen(new JsonDecoder())
                        .cacheable()
                        .cachePool(CacheNames.NEOFORGE_VERSION_META)
        .andThen(NeoForgeVersionList::fromJson));
    }

    @Override
    public CombinedDownloadTask<String> gameVersionJson(final MinecraftDirectory mcdir, String version) {
        final NeoForgeVersion neoForgeInfo = NeoForgeVersion.resolve(version);

        if (neoForgeInfo != null) {
            // for old forge versions
            return neoForgeVersion(neoForgeInfo)
                    .andThenDownload(forge -> CombinedDownloadTask.any(
                            installerTask(forge)
                                    .andThen(new InstallProfileProcessor(mcdir, FORGE_ARTIFACT_ID + "-installer.jar")),
                            upstreamProvider.gameVersionJson(mcdir, forge.getMinecraftVersion())
                                    .andThen(superversion -> createForgeVersionJson(mcdir, forge))
                                    .andThen(new VersionJsonInstaller(mcdir))));
        }

        return null;
    }

    @Override
    public CombinedDownloadTask<Void> library(final MinecraftDirectory mcdir, final Library library) {
        return null;
    }

    @Override
    public CombinedDownloadTask<Void> gameJar(final MinecraftDirectory mcdir, final Version version) {
        final NeoForgeVersion neoForgeInfo = NeoForgeVersion.resolve(version.getRoot());
        if (neoForgeInfo == null) {
            return null;
        }

        // downloads the super version
        CombinedDownloadTask<Version> baseTask = downloadSuperVersion(mcdir, neoForgeInfo.getMinecraftVersion());

        final File targetJar = mcdir.getVersionJar(version);

        // copy its superversion's jar
        // remove META-INF
        return baseTask.andThen(superVersion -> {
            purgeMetaInf(mcdir.getVersionJar(superVersion), targetJar);
            return null;
        });

    }

    @Override
    public void setUpstreamProvider(MinecraftDownloadProvider upstreamProvider) {
        this.upstreamProvider = upstreamProvider;
    }

    protected CombinedDownloadTask<byte[]> installerTask(NeoForgeVersion neoForgeVersion) {
        Library lib = new Library(FORGE_GROUP_ID, FORGE_ARTIFACT_ID, neoForgeVersion.getNeoForgeVersion(), CLASSIFIER_INSTALLER, "jar");
        return CombinedDownloadTask.single(
                new MemoryDownloadTask(source.getNeoForgeMavenRepositoryUrl() + lib.getPath())
                        .cacheable()
                        .cachePool(CacheNames.FORGE_INSTALLER));
    }

    protected JSONObject createForgeVersionJson(MinecraftDirectory mcdir, NeoForgeVersion neoForgeVersion) throws IOException, JSONException {
        JSONObject versionjson = IOUtils.toJson(mcdir.getVersionJson(neoForgeVersion.getMinecraftVersion()));

        versionjson.remove("downloads");
        versionjson.remove("assets");
        versionjson.remove("assetIndex");
        versionjson.put("id", neoForgeVersion.getVersionName());
        versionjson.put("mainClass", MINECRAFT_MAINCLASS);
        return versionjson;
    }

    protected void mergeJar(File parent, File universal, File target) throws IOException {
        FileUtils.prepareWrite(target);
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(parent.toPath()));
             ZipInputStream universalIn = new ZipInputStream(Files.newInputStream(universal.toPath()));
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target.toPath()))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            int read;

            Set<String> universalEntries = new HashSet<>();

            while ((entry = universalIn.getNextEntry()) != null) {
                universalEntries.add(entry.getName());
                out.putNextEntry(entry);
                while ((read = universalIn.read(buf)) != -1) {
                    out.write(buf, 0, read);
                }
                out.closeEntry();
                universalIn.closeEntry();
            }

            while ((entry = in.getNextEntry()) != null) {
                if (isNotMetaInfEntry(entry) && !universalEntries.contains(entry.getName())) {
                    out.putNextEntry(entry);
                    while ((read = in.read(buf)) != -1) {
                        out.write(buf, 0, read);
                    }
                    out.closeEntry();
                }
                in.closeEntry();
            }
        }
    }

    protected void purgeMetaInf(File src, File target) throws IOException {
        FileUtils.prepareWrite(target);
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(src.toPath()));
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target.toPath()))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            int read;
            while ((entry = in.getNextEntry()) != null) {
                if (isNotMetaInfEntry(entry)) {
                    out.putNextEntry(entry);
                    while ((read = in.read(buf)) != -1) {
                        out.write(buf, 0, read);
                    }
                    out.closeEntry();
                }
                in.closeEntry();
            }
        }
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

    private boolean isNotMetaInfEntry(ZipEntry entry) {
        return !entry.getName().startsWith("META-INF/");
    }

    private CombinedDownloadTask<Version> downloadSuperVersion(final MinecraftDirectory mcdir, String version) {
        return upstreamProvider.gameVersionJson(mcdir, version)
                .andThenDownload(resolvedMcversion -> {
                    final Version superversion = Versions.resolveVersion(mcdir, resolvedMcversion);
                    return upstreamProvider.gameJar(mcdir, superversion).andThenReturn(superversion);
                });
    }

}
