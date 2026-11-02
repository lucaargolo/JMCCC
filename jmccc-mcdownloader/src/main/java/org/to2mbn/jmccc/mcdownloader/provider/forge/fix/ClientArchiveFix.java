package org.to2mbn.jmccc.mcdownloader.provider.forge.fix;

import org.to2mbn.jmccc.mcdownloader.download.cache.CacheNames;
import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.tasks.FileDownloadTask;
import org.to2mbn.jmccc.mcdownloader.provider.forge.ForgeDownloadSource;
import org.to2mbn.jmccc.version.Library;

import java.io.File;

public final class ClientArchiveFix {

    private ClientArchiveFix() {
    }

    public static CombinedDownloadTask<Void> download(ForgeDownloadSource source, String mavenVersion, File target) {
        Library jar = new Library("net.minecraftforge", "forge", mavenVersion, "client", "jar");
        Library zip = new Library("net.minecraftforge", "forge", mavenVersion, "client", "zip");
        return CombinedDownloadTask.any(
                CombinedDownloadTask.single(new FileDownloadTask(source.getForgeMavenRepositoryUrl() + jar.getPath(), target).cachePool(CacheNames.FORGE_UNIVERSAL)),
                CombinedDownloadTask.single(new FileDownloadTask(source.getForgeMavenRepositoryUrl() + zip.getPath(), target).cachePool(CacheNames.FORGE_UNIVERSAL)));
    }
}
