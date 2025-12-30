package org.to2mbn.jmccc.mcdownloader.provider.neoforge;

public class DefaultNeoForgeDownloadSource implements NeoForgeDownloadSource {
    @Override
    public String getNeoForgeMetadataUrl() {
        return "https://maven.neoforged.net/api/maven/versions/releases/net%2Fneoforged%2Fneoforge";
    }

    @Override
    public String getNeoForgeMavenRepositoryUrl() {
        return "https://maven.neoforged.net/releases/";
    }
}
