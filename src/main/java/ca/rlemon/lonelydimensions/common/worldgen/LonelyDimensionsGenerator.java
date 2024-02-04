package ca.rlemon.lonelydimensions.common.worldgen;

import ca.rlemon.lonelydimensions.common.CommonConfig;
import ca.rlemon.lonelydimensions.common.LonelyDimensions;
import com.google.common.collect.ImmutableList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.Nullable;
import oshi.util.tuples.Pair;

import java.io.IOException;
import java.util.*;

public class LonelyDimensionsGenerator {

    public static ServerLevel createAndRegisterWorldAndDimension(MinecraftServer server, Map<ResourceKey<Level>, ServerLevel> map, ResourceKey<Level> worldKey, int index) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        LevelStem dimension = new LevelStem(overworld.dimensionTypeRegistration(), overworld.getChunkSource().getGenerator(), false);
        WorldData serverConfig = server.getWorldData();
        WorldGenSettings dimensionGeneratorSettings = serverConfig.worldGenSettings();
        ResourceKey<LevelStem> dimensionKey = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, worldKey.location());
        Registry.register(dimensionGeneratorSettings.dimensions(), dimensionKey.location(), dimension);
        DerivedLevelData derivedWorldInfo = new DerivedLevelData(serverConfig, serverConfig.overworldData());
        ServerLevel newWorld = new ServerLevel(server, server.executor, server.storageSource, derivedWorldInfo, worldKey, dimension.typeHolder(), server.progressListenerFactory.create(11), dimension.generator(), dimensionGeneratorSettings.isDebug(), BiomeManager.obfuscateSeed(dimensionGeneratorSettings.seed()), ImmutableList.of(), false);
        overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(newWorld.getWorldBorder()));
        map.put(worldKey, newWorld);
        server.markWorldsDirty();
        MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(newWorld));
        return newWorld;
    }

    @Nullable
    public static Pair<BoundingBox, BlockPos> generateStartingStructure(ServerLevel serverLevel, BlockPos blockPos) {

        // Load the NBT structure from the file
        ResourceLocation islandLocation = new ResourceLocation(LonelyDimensions.MOD_ID, "structures/vh_starting_structure.nbt");

        CompoundTag structureTag = loadStructureFromFile(serverLevel, islandLocation);

        if (structureTag != null) {

            // Paste the NBT structure at the player's spawn location
            StructureTemplate template = new StructureTemplate();
            template.load(structureTag);
            BoundingBox box = template.getBoundingBox(blockPos, Rotation.NONE, blockPos, Mirror.NONE);
            BlockPos placedPos = blockPos.below(box.getYSpan()).north(box.getZSpan()/2).west(box.getXSpan()/2);
            template.placeInWorld(serverLevel, placedPos, placedPos, new StructurePlaceSettings(), serverLevel.random, 10);

            return new Pair<>(box, placedPos);
        }
        return null;
    }

    private static CompoundTag loadStructureFromFile(ServerLevel serverLevel, ResourceLocation resourceLocation) {
        try {
            return NbtIo.readCompressed(serverLevel.getServer().getResourceManager().getResource(resourceLocation).getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void generateSimpleStartingStructure(ServerLevel serverLevel, BlockPos blockpos) {
        int i = blockpos.getX();
        int j = blockpos.getY() - 2;
        int k = blockpos.getZ();
        BlockPos.betweenClosed(i - 2, j + 1, k - 2, i + 2, j + 3, k + 2).forEach((p_207578_) -> {
            serverLevel.setBlockAndUpdate(p_207578_, Blocks.AIR.defaultBlockState());
        });
        BlockPos.betweenClosed(i - 2, j, k - 2, i + 2, j, k + 2).forEach((p_184101_) -> {
            serverLevel.setBlockAndUpdate(p_184101_, Blocks.OBSIDIAN.defaultBlockState());
        });
    }

    public static BlockPos movePlayerToLowestValidPositionFromOffset(ServerLevel serverLevel, ServerPlayer serverplayer, int offset) {
        serverplayer.setPos(0, CommonConfig.SPAWN_Y.get() - offset, 0);
        while(!serverLevel.noCollision(serverplayer) && serverplayer.getY() < (double)serverLevel.getMaxBuildHeight()) {
            serverplayer.setPos(serverplayer.getX(), serverplayer.getY() + 1.0D, serverplayer.getZ());
        }
        return serverplayer.getOnPos();
    }

    @Deprecated
    public static BlockPos findEntryPoint(ServerPlayer player, ServerLevel serverLevel, int yCheckLimit) {
        BlockPos actualPlacePos = player.getOnPos();
        if( CommonConfig.USE_SPAWN_STRUCTURE.get() == Boolean.TRUE ) {
            actualPlacePos = new BlockPos(0, CommonConfig.SPAWN_Y.get(), 0);
            boolean flag = false;
            for (int y = 1; y < yCheckLimit && !flag; y++) {

                int x = RandomUtils.nextInt(0, 2) - 1;
                int z = RandomUtils.nextInt(0, 2) - 1;
                BlockPos testPos = actualPlacePos.above(y).north(z).east(x);
                if (serverLevel.getBlockState(testPos).isAir() && !serverLevel.getBlockState(testPos.below()).isAir()) {
                    flag = true;
                    actualPlacePos = testPos;
                }
            }
        }
        return actualPlacePos;
    }
}
