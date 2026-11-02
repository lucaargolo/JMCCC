package org.to2mbn.jmccc.mcdownloader.provider.forge.fix;

import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadTask;
import org.to2mbn.jmccc.mcdownloader.provider.MinecraftDownloadProvider;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.util.FileUtils;
import org.to2mbn.jmccc.version.Library;
import org.to2mbn.jmccc.version.parsing.Versions;

public final class DeobfRealmsFix {

    private DeobfRealmsFix() {
    }

    public static CombinedDownloadTask<String> prepare(MinecraftDirectory directory, String minecraftVersion, MinecraftDownloadProvider provider) {
        CombinedDownloadTask<String> vanilla = directory.getVersionJson(minecraftVersion).isFile()
                ? CombinedDownloadTask.multiple(new CombinedDownloadTask<?>[0]).andThenReturn(minecraftVersion)
                : provider.gameVersionJson(directory, minecraftVersion);
        return vanilla.andThen(resolved -> {
            for (Library library : Versions.resolveVersion(directory, resolved).getLibraries()) {
                if ("com.mojang".equals(library.getGroupId()) && "realms".equals(library.getArtifactId())) {
                    FileUtils.prepareWrite(directory.getLibrary(library));
                    break;
                }
            }
            return resolved;
        });
    }
}
