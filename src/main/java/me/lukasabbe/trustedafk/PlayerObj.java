package me.lukasabbe.trustedafk;

import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class PlayerObj {
    public UUID playerUUid;
    public int ticksAfk;
    public Vec3d last_pos;
    public boolean isAfk;

    public PlayerObj(UUID playerUUid) {
        this.playerUUid = playerUUid;
    }
}
