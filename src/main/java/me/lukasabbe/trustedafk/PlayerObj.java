package me.lukasabbe.trustedafk;

import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class PlayerObj {
    public UUID playerUUid;
    public int ticksAfk;
    public Vec3d lastPos;
    public Vec3d lastRot;
    public boolean isAfk;
    public String lastTeam = null;

    public PlayerObj(UUID playerUUid) {
        this.playerUUid = playerUUid;
    }
}
