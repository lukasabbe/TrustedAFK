package me.lukasabbe.trustedafk;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Trustedafk implements ModInitializer, ServerTickEvents.EndTick, ServerPlayConnectionEvents.Join, ServerPlayConnectionEvents.Disconnect{

    private final List<PlayerObj> PLAYERS = new ArrayList<>();
    private int tick_counter = 0;

    private boolean create_team = true;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(this);
        ServerPlayConnectionEvents.JOIN.register(this);
        ServerPlayConnectionEvents.DISCONNECT.register(this);
        CommandRegistrationCallback.EVENT.register(
                (commandDispatcher, commandRegistryAccess, registrationEnvironment) ->
                        commandDispatcher.register(CommandManager.literal("afk").executes((c)->{
                            if(!c.getSource().isExecutedByPlayer()){
                                c.getSource().sendError(Text.of("only player command"));
                                return 1;
                            }
                            final ServerPlayerEntity player = c.getSource().getPlayer();
                            Optional<PlayerObj> optionalPlayer = PLAYERS.stream().filter(u -> u.playerUUid == player.getUuid()).findFirst();
                            if(optionalPlayer.isEmpty()) return 1;
                            PlayerObj playerObj = optionalPlayer.get();
                            toggleAfk(player,playerObj);
                            return 1;
                        })));
    }


    @Override
    public void onEndTick(MinecraftServer minecraftServer) {
        tick_counter++;
        if(tick_counter >= 20) return;
        tick_counter = 0;
        if(create_team){
            if(minecraftServer.getScoreboard().getTeam("trusted_afk") == null)
                minecraftServer.getScoreboard().addTeam("trusted_afk");
            minecraftServer.getScoreboard().getTeam("trusted_afk").setColor(Formatting.GRAY);
            minecraftServer.getScoreboard().getTeam("trusted_afk").setPrefix(Text.of("[AFK]"));
            create_team = false;
        }
        minecraftServer.getPlayerManager().getPlayerList().forEach(player -> {
            Optional<PlayerObj> optionalPlayer = PLAYERS.stream().filter(u -> u.playerUUid == player.getUuid()).findFirst();
            if(optionalPlayer.isEmpty()) return;
            PlayerObj playerObj = optionalPlayer.get();
            if(playerObj.last_pos != null && playerObj.last_pos.equals(player.getPos())){
                playerObj.ticksAfk++;
                if(playerObj.ticksAfk > 20*10 && !playerObj.isAfk){
                    toggleAfk(player,playerObj);
                }

            }else {
                playerObj.ticksAfk = 0;
                if(playerObj.isAfk)
                    toggleAfk(player,playerObj);
            }
            playerObj.last_pos = player.getPos();
        });
    }

    public void toggleAfk(ServerPlayerEntity player, PlayerObj playerObj){
        playerObj.isAfk = !playerObj.isAfk;
        if(playerObj.isAfk){
            player.getScoreboard().addScoreHolderToTeam(player.getNameForScoreboard(),player.getScoreboard().getTeam("trusted_afk"));
        }else{
            player.getScoreboard().clearTeam(player.getNameForScoreboard());
        }
    }

    @Override
    public void onPlayReady(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        PLAYERS.add(new PlayerObj(serverPlayNetworkHandler.getPlayer().getUuid()));
    }

    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        PLAYERS.removeIf(u -> {
            if(u.playerUUid == serverPlayNetworkHandler.getPlayer().getUuid()){
                if(u.isAfk)
                    toggleAfk(serverPlayNetworkHandler.player,u);
                return true;
            }
            return false;
        });
    }
}
