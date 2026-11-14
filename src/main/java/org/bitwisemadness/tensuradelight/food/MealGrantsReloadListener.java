package org.bitwisemadness.tensuradelight.food;

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
 * Loads {@code data/<mod id>/meal_grant/*.json} into {@link MealGrants}.
 *
 * <p>Registered on NeoForge's {@code AddReloadListenerEvent}, so it runs on
 * world load and on {@code /reload}: a pack can retune the grants without
 * restarting the server. Files are read as text and handed to
 * {@link MealGrants#parse}, which reports what it skipped - one malformed
 * file never stops the reload or the files beside it.</p>
 */
public final class MealGrantsReloadListener implements ResourceManagerReloadListener {

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Map<String, String> files = new TreeMap<>();
        Map<ResourceLocation, Resource> found = resourceManager.listResources(
                MealGrants.DIRECTORY,
                id -> TensuraDelight.MOD_ID.equals(id.getNamespace()));
        found.forEach((id, resource) -> readInto(files, id, resource));
        if (files.isEmpty()) {
            TensuraDelight.LOGGER.warn("No meal_grant data found; no meal grants EP or magicule");
        }
        MealGrants.ParseResult result = MealGrants.parse(files);
        for (String problem : result.problems()) {
            TensuraDelight.LOGGER.warn("Skipping malformed meal_grant file: {}", problem);
        }
        MealGrants.replace(result.table());
        TensuraDelight.LOGGER.info("Loaded {} meal(s) and {} override(s) granting EP and magicule",
                result.table().meals().size(), result.table().overrides().size());
    }

    private static void readInto(Map<String, String> files, ResourceLocation id, Resource resource) {
        try (InputStream in = resource.open()) {
            files.put(id.toString(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException failure) {
            TensuraDelight.LOGGER.warn("Could not read {}", id, failure);
        }
    }
}
