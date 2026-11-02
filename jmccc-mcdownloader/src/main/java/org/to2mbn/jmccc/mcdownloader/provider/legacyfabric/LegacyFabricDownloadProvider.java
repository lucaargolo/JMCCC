package org.to2mbn.jmccc.mcdownloader.provider.legacyfabric;

import org.to2mbn.jmccc.mcdownloader.provider.fabric.FabricDownloadProvider;
import org.to2mbn.jmccc.mcdownloader.provider.fabric.FabricDownloadSource;

public class LegacyFabricDownloadProvider extends FabricDownloadProvider {
    public LegacyFabricDownloadProvider() {
        this(new LegacyFabricDownloadSource());
    }

    public LegacyFabricDownloadProvider(FabricDownloadSource source) {
        super(source);
    }

    @Override
    protected String getLoaderName() {
        return "legacyfabric";
    }
}
