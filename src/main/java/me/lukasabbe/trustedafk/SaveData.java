package me.lukasabbe.trustedafk;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SaveData extends PersistentState {
    public Map<UUID, SavePlayerData> players = new HashMap<>();

    public static SavePlayerData getPlayerState(ServerPlayerEntity player){
        SaveData data = getServerState(player.getWorld().getServer());
        return data.players.computeIfAbsent(player.getUuid(), uuid -> new SavePlayerData());
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound playersNbt = new NbtCompound();
        players.forEach(((uuid, playerData) -> {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putBoolean("trustedafk",playerData.optIn);
            playersNbt.put(uuid.toString(),playerNbt);
        }));
        nbt.put("players",playersNbt);
        return nbt;
    }
    public static SaveData createFromNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup registerLookup){
        SaveData data = new SaveData();
        NbtCompound playersNbt = nbtCompound.getCompound("players");
        playersNbt.getKeys().forEach(key ->{
            SavePlayerData playerData = new SavePlayerData();
            playerData.optIn = playersNbt.getCompound(key).getBoolean("trustedafk");

            UUID uuid = UUID.fromString(key);
            data.players.put(uuid,playerData);
        });
        return data;
    }
    private static Type<SaveData> type = new Type<>(
            SaveData::new,
            SaveData::createFromNbt,
            null
    );
    public static SaveData getServerState(MinecraftServer server){
        PersistentStateManager manager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        SaveData data = manager.getOrCreate(type, "trustedafk");
        data.markDirty();
        return data;
    }
}

