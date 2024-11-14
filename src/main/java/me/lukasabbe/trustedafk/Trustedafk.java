package me.lukasabbe.trustedafk;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class Trustedafk implements ModInitializer, ServerTickEvents.EndTick, ServerPlayConnectionEvents.Join, ServerPlayConnectionEvents.Disconnect, ServerMessageEvents.ChatMessage{
    private final Map<UUID, PlayerObj> PLAYERS = new HashMap<>();
    private int tick_counter = 0;
    public static final Config CONFIG = new Config();

    private boolean create_team = true;

    @Override
    public void onInitialize() {
        CONFIG.loadConfig();
        ServerTickEvents.END_SERVER_TICK.register(this);
        ServerPlayConnectionEvents.JOIN.register(this);
        ServerPlayConnectionEvents.DISCONNECT.register(this);
        ServerMessageEvents.CHAT_MESSAGE.register(this);
        CommandRegistrationCallback.EVENT.register(
                (commandDispatcher, commandRegistryAccess, registrationEnvironment) ->
                        commandDispatcher.register(CommandManager.literal("afk").executes((c)->{
                            if(!c.getSource().isExecutedByPlayer()){
                                c.getSource().sendError(Text.of("only player command"));
                                return 1;
                            }
                            final ServerPlayerEntity player = c.getSource().getPlayer();
                            PlayerObj playerObj = PLAYERS.get(player.getUuid());
                            toggleAfk(player,playerObj);
                            return 1;
                        })));
    }


    @Override
    public void onEndTick(MinecraftServer minecraftServer) {
        tick_counter++;
        if(tick_counter < 20) return;
        tick_counter = 0;
        if(create_team){
            if(minecraftServer.getScoreboard().getTeam("trusted_afk") == null)
                minecraftServer.getScoreboard().addTeam("trusted_afk");
            else{
                minecraftServer.getScoreboard().removeTeam(minecraftServer.getScoreboard().getTeam("trusted_afk"));
                minecraftServer.getScoreboard().addTeam("trusted_afk");
            }
            minecraftServer.getScoreboard().getTeam("trusted_afk").setColor(Formatting.GRAY);
            minecraftServer.getScoreboard().getTeam("trusted_afk").setPrefix(Text.of("[AFK]"));
            create_team = false;
        }
        minecraftServer.getPlayerManager().getPlayerList().forEach(player -> {
            PlayerObj playerObj = PLAYERS.get(player.getUuid());
            if(playerObj.lastPos != null && playerObj.lastPos.equals(player.getPos()) && playerObj.lastRot != null && playerObj.lastRot.equals(player.getCameraEntity().getRotationVector())){
                playerObj.ticksAfk+=20;
                if(playerObj.ticksAfk > 20*60*CONFIG.afkTime && !playerObj.isAfk){
                    toggleAfk(player,playerObj);
                }

            }else {
                playerObj.ticksAfk = 0;
                if(playerObj.isAfk)
                    toggleAfk(player,playerObj);
            }
            playerObj.lastPos = player.getPos();
            playerObj.lastRot = player.getCameraEntity().getRotationVector();
        });
    }
    public void toggleAfk(ServerPlayerEntity player, PlayerObj playerObj){
        playerObj.isAfk = !playerObj.isAfk;
        if(playerObj.isAfk){
            if(player.getScoreboardTeam() != null)
                playerObj.lastTeam = player.getScoreboardTeam().getName();
            player.getScoreboard().addScoreHolderToTeam(player.getNameForScoreboard(),player.getScoreboard().getTeam("trusted_afk"));
        }else{
            player.getScoreboard().clearTeam(player.getNameForScoreboard());
            if(playerObj.lastTeam != null)
                player.getScoreboard().addScoreHolderToTeam(player.getNameForScoreboard(),player.getScoreboard().getTeam(playerObj.lastTeam));
        }
    }

    @Override
    public void onPlayReady(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        PLAYERS.put(serverPlayNetworkHandler.player.getUuid(),new PlayerObj(serverPlayNetworkHandler.getPlayer().getUuid()));
    }

    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        toggleAfk(serverPlayNetworkHandler.player, PLAYERS.get(serverPlayNetworkHandler.getPlayer().getUuid()));
        PLAYERS.remove(serverPlayNetworkHandler.player.getUuid());
    }

    @Override
    public void onChatMessage(SignedMessage signedMessage, ServerPlayerEntity serverPlayerEntity, MessageType.Parameters parameters) {
        if(serverPlayerEntity.isPlayer()){
            final PlayerObj playerObj = PLAYERS.get(serverPlayerEntity.getUuid());
            playerObj.ticksAfk = 0;
            if(playerObj.isAfk)
                toggleAfk(serverPlayerEntity,playerObj);
        }
    }
}
