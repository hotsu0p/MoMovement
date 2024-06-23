package hotsuop.momovement;

import hotsuop.momovement.config.MoMovementConfig;
import hotsuop.momovement.config.IMoMovementConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import hotsuop.momovement.api.SoundRegistry;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class MoMovement implements ModInitializer {
    public static final String MOD_ID = "momovement";
    protected static MoMovementConfig serverConfig = null;
    public static MoMovementConfig getConfig() {
        if(serverConfig != null) return serverConfig;
        return CONFIG.getConfig();
    }
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Identifier MOVE_STATE = new Identifier(MOD_ID, "move_state");
    public static final Identifier CONFIG_STATE = new Identifier(MOD_ID, "config_state");
    private static final Object _queueLock = new Object();
    private static final Queue<Runnable> _actionQueue = new LinkedList<>();
    public static IMoveStateUpdater moveStateUpdater;
    public static IMoMovementInput INPUT;
    public static IMoMovementConfig CONFIG;

    @Override
    public void onInitialize() {
        SoundRegistry.registerSound();
        moveStateUpdater = new IMoveStateUpdater(){
            @Override
            public void setMoveState(PlayerEntity player, MoveState moveState) {}
            @Override
            public void setAnimationState(PlayerEntity player, MoveState moveState) {}
        };

        INPUT = new IMoMovementInput() {
            @Override
            public boolean ismoveUpKeyPressed() {return false;}
            @Override
            public boolean ismoveDownKeyPressed() {return false;}
            @Override
            public boolean ismoveUpKeyPressedLastTick() {return false;}
            @Override
            public boolean ismoveDownKeyPressedLastTick() {return false;}
        };

        CONFIG = MoMovementConfig::new;

        ServerPlayNetworking.registerGlobalReceiver(MOVE_STATE, (server, player, handler, buf, responseSender) -> {
            var uuid = buf.readUuid();
            var moveStateInt = buf.readInt();
            MoveState moveState = MoveState.STATE(moveStateInt);
            IMoPlayer moPlayer = (IMoPlayer) server.getPlayerManager().getPlayer(uuid);
            if( moPlayer != null) moPlayer.momovement_setMoveState(moveState);

            SendToClients((PlayerEntity) moPlayer, MOVE_STATE, uuid, moveStateInt);

        });

        ServerTickEvents.END_SERVER_TICK.register((server) -> {
            synchronized (_queueLock) {
                while (_actionQueue.size() > 0) {
                    _actionQueue.poll().run();
                }
            }
        });
        //send config to clients on join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var buf = PacketByteBufs.create();
            buf.writeBoolean(getConfig().enableMoMovement);
            buf.writeBoolean(getConfig().diveRollEnabled);
            buf.writeInt(getConfig().diveRollStaminaCost);
            buf.writeDouble(getConfig().diveRollSpeedBoostMultiplier);
            buf.writeInt(getConfig().diveRollCoolDown);
            buf.writeBoolean(getConfig().diveRollWhenSwimming);
            buf.writeBoolean(getConfig().diveRollWhenFlying);
            buf.writeBoolean(getConfig().wallRunEnabled);
            buf.writeInt(getConfig().wallRunStaminaCost);
            buf.writeDouble(getConfig().wallRunSpeedBoostMultiplier);
            buf.writeInt(getConfig().wallRunDurationTicks);
            buf.writeBoolean(getConfig().slideEnabled);
            buf.writeInt(getConfig().slideStaminaCost);
            buf.writeDouble(getConfig().slideSpeedBoostMultiplier);
            buf.writeInt(getConfig().slideCoolDown);
            sender.sendPacket(CONFIG_STATE, buf);
        });

    }

    public static void SendToClients(PlayerEntity source, Identifier type, UUID uuid, int moveStateInt){
        synchronized (_queueLock) {
            _actionQueue.add(() -> {
                for (PlayerEntity target : source.getServer().getPlayerManager().getPlayerList()) {
                    if (target != source && target.squaredDistanceTo(source) < 6400) {
                        var buf = PacketByteBufs.create();
                        buf.writeUuid(uuid);
                        buf.writeInt(moveStateInt);
                        ServerPlayNetworking.send((ServerPlayerEntity) target, type, buf);
                    }
                }
            });
        }
    }

}
