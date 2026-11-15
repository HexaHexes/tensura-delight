package org.bitwisemadness.tensuradelight.gear;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.bitwisemadness.tensuradelight.TensuraDelight;

/**
 * Loads {@code data/<mod id>/ep_per_cut/*.json} into {@link CuttingGrants}.
 *
 * <p>Registered beside the meal grant listener on NeoForge's
 * {@code AddReloadListenerEvent}, so a pack can retune the per-cut grant on
 * world load and on {@code /reload}. Files are read as text and handed to
 * {@link CuttingGrants#parse}, which reports what it skipped - one malformed
 * file never stops the reload or the files beside it.</p>
 */
public final class CuttingGrantsReloadListener implements ResourceManagerReloadListener {

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Map<String, String> files = new TreeMap<>();
        Map<ResourceLocation, Resource> found = resourceManager.listResources(
                CuttingGrants.DIRECTORY,
                id -> TensuraDelight.MOD_ID.equals(id.getNamespace()));
        found.forEach((id, resource) -> readInto(files, id, resource));
        CuttingGrants.ParseResult result = CuttingGrants.parse(files);
        for (String problem : result.problems()) {
            TensuraDelight.LOGGER.warn("Skipping malformed ep_per_cut file: {}", problem);
        }
        CuttingGrants.replace(result.table());
        TensuraDelight.LOGGER.info("Cutting board grant set to {} EP per cut", result.table().epPerCut());
    }

    private static void readInto(Map<String, String> files, ResourceLocation id, Resource resource) {
        try (InputStream in = resource.open()) {
            files.put(id.toString(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException failure) {
            TensuraDelight.LOGGER.warn("Could not read {}", id, failure);
        }
    }
}
