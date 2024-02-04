package ca.rlemon.lonelydimensions.common;


import ca.rlemon.lonelydimensions.common.worldgen.LonelyDimensionsGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.RandomUtils;
import oshi.util.tuples.Pair;

import java.util.Objects;


@Mod.EventBusSubscriber(modid = LonelyDimensions.MOD_ID)
public class EventBusSubscriber {

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if( event.getPlayer() instanceof ServerPlayer player ) {
            MinecraftServer server = event.getEntity().getServer();
            ResourceKey<Level> levelKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(LonelyDimensions.MOD_ID, player.getUUID().toString()));
            assert server != null;
            if (server.levelKeys().contains(levelKey)) {
                ServerLevel serverLevel = server.getLevel(levelKey);
                assert serverLevel != null;
                BlockPos respawnPosition = player.getRespawnPosition();
                if( respawnPosition == null ) {
                    LonelyDimensions.LOGGER.info("getting respawn position from mananger for {}", player.getName());
                    respawnPosition = PlayerRespawnManager.get(serverLevel).getRespawnPosition(player.getUUID());
                    if( respawnPosition == null ) {
                        LonelyDimensions.LOGGER.info("getting respawn position from generator for {}", player.getName());
                        respawnPosition = LonelyDimensionsGenerator.movePlayerToLowestValidPositionFromOffset(serverLevel, player, 0);
                        player.setRespawnPosition(serverLevel.dimension(), player.getOnPos().above(), player.getRespawnAngle(), true, false);
                    }
                    player.teleportTo(serverLevel, respawnPosition.getX(), respawnPosition.getY() + 1, respawnPosition.getZ(), player.getXRot(), player.getYRot()); // this is redundant on the last check but idk how to improve the logic right now.
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeup(PlayerWakeUpEvent event) {

        Level level = event.getPlayer().getLevel();
        if( !level.isClientSide && !level.dimension().equals(Level.OVERWORLD)) {
            MinecraftServer server = Objects.requireNonNull(level.getServer());
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (event.getPlayer().isSleepingLongEnough()) {
                if (overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
                    long j = overworld.getDayTime() + 24000L;
                    overworld.setDayTime(net.minecraftforge.event.ForgeEventFactory.onSleepFinished(overworld, j - j % 24000L, overworld.getDayTime()));
                }
                // this crashes servers when not using vanilla beds?? why??
                // server.getPlayerList().getPlayers().forEach(player -> player.stopSleepInBed(false, false));
                if (overworld.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE) && overworld.isRaining()) {
                    overworld.resetWeatherCycle();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerSetRespawn(PlayerSetSpawnEvent event) {
        if( event.getPlayer() instanceof ServerPlayer player ) {
            ResourceKey<Level> levelKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(LonelyDimensions.MOD_ID, player.getUUID().toString()));
            if( !player.getLevel().dimension().equals(levelKey) ) {
                event.setCanceled(true);
                if( player.isSleeping() ) {
                    player.stopSleeping();
                }
                BlockPos attemptedBed = event.getNewSpawn();
                player.getLevel().explode(null, DamageSource.badRespawnPointExplosion(), null, attemptedBed.getX() + 0.5D, attemptedBed.getY() + 0.5D, attemptedBed.getZ() + 0.5D, 5.0F, true, Explosion.BlockInteraction.NONE);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if( event.getPlayer() instanceof ServerPlayer player ) {
            MinecraftServer server = event.getEntity().getServer();
            ResourceKey<Level> levelKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(LonelyDimensions.MOD_ID, player.getUUID().toString()));
            if( !server.levelKeys().contains(levelKey) ) {
                ServerLevel serverLevel = LonelyDimensionsGenerator.createAndRegisterWorldAndDimension(server, server.forgeGetWorldMap(), levelKey, 1);
                BlockPos actualPlacePos = player.getOnPos();
                if( CommonConfig.USE_SPAWN_STRUCTURE.get() == Boolean.TRUE ) {
                    BlockPos blockPos = new BlockPos(0, CommonConfig.SPAWN_Y.get(), 0);
                    Pair<BoundingBox, BlockPos> positioning = LonelyDimensionsGenerator.generateStartingStructure(serverLevel, blockPos);
                    if( positioning != null ) {
                        BoundingBox size = positioning.getA();
                        BlockPos pos = positioning.getB();
                        actualPlacePos = new BlockPos(blockPos.getX(), pos.getY(), blockPos.getZ());
                        boolean flag = false;
                        for (int y = 1; y < size.getYSpan() && !flag; y++) {

                            int x = RandomUtils.nextInt(0, 2) - 1;
                            int z = RandomUtils.nextInt(0, 2) - 1;
                            BlockPos testPos = actualPlacePos.above(y).north(z).east(x);
                            if (serverLevel.getBlockState(testPos).isAir() && !serverLevel.getBlockState(testPos.below()).isAir()) {
                                flag = true;
                                actualPlacePos = testPos;
                            }
                        }
                    } else {
                        LonelyDimensionsGenerator.generateSimpleStartingStructure(serverLevel, player.getOnPos());
                    }
                }
                PlayerRespawnManager.get(serverLevel).setRespawnPosition(player.getUUID(), actualPlacePos);
                player.teleportTo(serverLevel, actualPlacePos.getX(), actualPlacePos.getY() + 1, actualPlacePos.getZ(), player.getXRot(), player.getYRot());
                player.setRespawnPosition(serverLevel.dimension(), actualPlacePos.above(), player.getRespawnAngle(), true, false);
            }
        }
    }
}
