package ca.rlemon.lonelydimensions.common;

import ca.rlemon.lonelydimensions.common.worldgen.LonelyDimensionsGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerRespawnManager extends SavedData {

    public static final String DATANAME = "lonelydimensionrespawndata";

    public HashMap<UUID, BlockPos> positions = new HashMap<>();

    public static PlayerRespawnManager load(CompoundTag source) {
        PlayerRespawnManager manager = new PlayerRespawnManager();
        manager.positions.clear();
        ListTag list = source.getList("list", 10);
        for(Tag entry : list ) {
            CompoundTag tag = (CompoundTag) entry;
            UUID id = tag.getUUID("uuid");
            BlockPos pos = BlockPos.of(tag.getLong("pos"));
            manager.positions.put(id, pos);
        }
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag source) {
        ListTag list = new ListTag();
        source.put("list", list);
        for(Map.Entry<UUID, BlockPos> entry : positions.entrySet() ) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("uuid",entry.getKey());
            tag.putLong("pos",entry.getValue().asLong());
        }
        return source;
    }

    public static PlayerRespawnManager get(ServerLevel serverLevel) {
        DimensionDataStorage store = serverLevel.getDataStorage();
        PlayerRespawnManager manager = store.computeIfAbsent(PlayerRespawnManager::load, PlayerRespawnManager::new, DATANAME);
        if( manager == null ) {
            LonelyDimensions.LOGGER.info("manager is null");
            manager = new PlayerRespawnManager();
            store.set(DATANAME, manager);
        }
        return manager;
    }

    public BlockPos getRespawnPosition(UUID uuid) {
        return positions.get(uuid);
    }
    public void setRespawnPosition(UUID uuid, BlockPos pos) {
        positions.put(uuid, pos);
    }

    @Override
    public boolean isDirty() {
        return true;
    }
}
