package org.to2mbn.jmccc.mcdownloader.provider.legacyfabric;

import org.to2mbn.jmccc.mcdownloader.provider.fabric.FabricDownloadSource;

public class LegacyFabricDownloadSource implements FabricDownloadSource {
    @Override
    public String getFabricMetaBaseUrl() {
        return "https://meta.legacyfabric.net/v2/";
    }
}
