package club.sk1er.patcher.hooks;

import club.sk1er.patcher.config.PatcherConfig;
import club.sk1er.patcher.database.AssetsDatabase;
import club.sk1er.patcher.database.DatabaseReturn;
import club.sk1er.patcher.tweaker.asm.FallbackResourceManagerTransformer;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleResource;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Used in {@link FallbackResourceManagerTransformer#transform(ClassNode, String)}
 */
@SuppressWarnings("unused")
public class FallbackResourceManagerHook {
    public static final Set<String> negativeResourceCache = new HashSet<>();
    private static final AssetsDatabase database = new AssetsDatabase();

    static {
        try {
            negativeResourceCache.addAll(database.getAllNegative());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void clearCache() {
        negativeResourceCache.clear();
        database.clearAll();
    }

    public static IResource getCachedResource(final FallbackResourceManager manager, final ResourceLocation location) throws IOException {
        if (negativeResourceCache.contains(location.toString())) {
            throw new FileNotFoundException(location.toString());
        }
        ResourceLocation mcMetaLocation = FallbackResourceManager.getLocationMcmeta(location);
        if (PatcherConfig.cachedResources) {
            DatabaseReturn data = database.getData(location.toString());
            if (data != null) {
                return new SimpleResource(data.getPackName(),
                    location,
                    new ByteArrayInputStream(data.getData()),
                    data.getMcMeta() != null ? new ByteArrayInputStream(data.getMcMeta()) : null,
                    manager.frmMetadataSerializer);
            }
        }
        byte[] rawMcMeta = null;
        for (int i = manager.resourcePacks.size() - 1; i >= 0; --i) {
            IResourcePack currentPack = manager.resourcePacks.get(i);

            if (rawMcMeta == null) {
                InputStream safe = getFromFile(currentPack, mcMetaLocation);
                if (safe != null) {
                    rawMcMeta = readCopy(safe);
                }
            }

            InputStream stream = getFromFile(currentPack, location);
            if (stream != null) {
                InputStream mcMetaData = null;
                if (rawMcMeta != null) {
                    mcMetaData = new ByteArrayInputStream(rawMcMeta);
                }
                byte[] mainData = readCopy(stream);
                if (PatcherConfig.cachedResources)
                    database.update(currentPack.getPackName(), location.toString(), mainData, rawMcMeta);
                return new SimpleResource(
                    currentPack.getPackName(),
                    location,
                    new ByteArrayInputStream(mainData),
                    mcMetaData,
                    manager.frmMetadataSerializer);
            }
        }
        negativeResourceCache.add(location.getResourcePath());
        throw new FileNotFoundException(location.toString());
    }

    private static byte[] readCopy(InputStream inputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, out);
        inputStream.close();
        return out.toByteArray();
    }

    private static InputStream getFromFile(IResourcePack pack, ResourceLocation location) {

        try {
            BufferedInputStream inputStream = new BufferedInputStream(pack.getInputStream(location));
            byte[] bytes = readCopy(inputStream);
            return new ByteArrayInputStream(bytes);
        } catch (Throwable ignored) {
        }

        return null;
    }

    static class Data {
        String name;
        ResourceLocation location;
        byte[] stream;
        byte[] mcMeta;

        public Data(String name, ResourceLocation location, byte[] stream, byte[] mcMeta) {
            this.name = name;
            this.location = location;
            this.stream = stream;
            this.mcMeta = mcMeta;
        }

        @Override
        public String toString() {
            return "Data{" +
                "name='" + name + '\'' +
                ", location=" + location +
                ", stream=" + Arrays.toString(stream) +
                ", mcMeta=" + Arrays.toString(mcMeta) +
                '}';
        }
    }
}
