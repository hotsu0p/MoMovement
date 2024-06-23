package hotsuop.momovement.client;

import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import hotsuop.momovement.*;
import hotsuop.momovement.config.MoMovementConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.uku3lig.ukulib.config.ConfigManager;
import java.util.Map;

public class MoMovementClient extends MoMovement implements ClientModInitializer {
    private static final Map<String, KeyframeAnimation> _animations = new java.util.HashMap<>();
    public static final ConfigManager<MoMovementConfig> CONFIG_MANAGER = ConfigManager.createDefault(MoMovementConfig.class, MoMovement.MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("initializing MoMovement Client :3");

        MoMovementInput input = new MoMovementInput();
        INPUT = input;
        ClientTickEvents.END_CLIENT_TICK.register(input::onEndTick);

        moveStateUpdater = new IMoveStateUpdater(){
            @Override
            public void setMoveState(PlayerEntity player, MoveState moveState) {
                var buf = PacketByteBufs.create();
                buf.writeUuid(player.getUuid());
                buf.writeInt(MoveState.STATE(moveState));
                ClientPlayNetworking.send(MoMovement.MOVE_STATE, buf);
            }
            @Override
            public void setAnimationState(PlayerEntity player, MoveState moveState) {
                if(!((player) instanceof IAnimatedPlayer animatedPlayer)) return;
                //LOGGER.info("found IAnimatedPlayer");
                var animationContainer = animatedPlayer.momovement_getModAnimation();
                var animationBodyContainer = animatedPlayer.momovement_getModAnimationBody();
                if(animationContainer == null || animationBodyContainer == null) return;
                //LOGGER.info("found animationContainers");

                if(_animations.isEmpty()){
                    //LOGGER.info("loading animations");
                    for(var entry : MoveState.STATES.values()){
                        //LOGGER.info("trying animation: " + entry.name);
                        var name = entry.name;
                        if(name.equals("none")) continue;
                        KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(new Identifier(MOD_ID, entry.name));
                        if(animation == null) continue;
                        //LOGGER.info("found animation: " + entry.name);
                        _animations.put(entry.name, animation);
                    }
                }

                var fade = AbstractFadeModifier.standardFadeIn(10, Ease.INOUTQUAD);
                var anim = _animations.get(moveState.name);
                if(anim == null) {
                    animationBodyContainer.replaceAnimationWithFade(fade, null);
                    animationContainer.replaceAnimationWithFade(fade, null);
                    return;
                }
                //LOGGER.info("found animation");
                var bodyLayer = new KeyframeAnimationPlayer(anim);
                var bodyVal = bodyLayer.bodyParts.get("body");
                bodyLayer.bodyParts.clear();
                bodyLayer.bodyParts.put("body", bodyVal);
                animationBodyContainer.replaceAnimationWithFade(fade, bodyLayer);
                animationContainer.replaceAnimationWithFade(fade, new KeyframeAnimationPlayer(anim));
            }
        };

        CONFIG = CONFIG_MANAGER::getConfig;

        //register receivers
        ClientPlayNetworking.registerGlobalReceiver(MoMovement.MOVE_STATE, (client, handler, buf, responseSender) -> {
            if (client.world != null) {
                var uuid = buf.readUuid();
                var moveStateInt = buf.readInt();
                MoveState moveState = MoveState.STATE(moveStateInt);
                IMoPlayer moPlayer = (IMoPlayer) client.world.getPlayerByUuid(uuid);
                if (moPlayer != null) moPlayer.momovement_setMoveState(moveState);
            }
        });

        //Set config on join
        ClientPlayNetworking.registerGlobalReceiver(MoMovement.CONFIG_STATE, (client, handler, buf, responseSender) -> {
            serverConfig = new MoMovementConfig();
            serverConfig.enableMoMovement = buf.readBoolean();
            serverConfig.diveRollEnabled = buf.readBoolean();
            serverConfig.diveRollStaminaCost = buf.readInt();
            serverConfig.diveRollSpeedBoostMultiplier = buf.readDouble();
            serverConfig.diveRollCoolDown = buf.readInt();
            serverConfig.diveRollWhenSwimming = buf.readBoolean();
            serverConfig.diveRollWhenFlying = buf.readBoolean();
            serverConfig.wallRunEnabled = buf.readBoolean();
            serverConfig.wallRunStaminaCost = buf.readInt();
            serverConfig.wallRunSpeedBoostMultiplier = buf.readDouble();
            serverConfig.wallRunDurationTicks = buf.readInt();
            serverConfig.slideEnabled = buf.readBoolean();
            serverConfig.slideStaminaCost = buf.readInt();
            serverConfig.slideSpeedBoostMultiplier = buf.readDouble();
            serverConfig.slideCoolDown = buf.readInt();
            LOGGER.info("Got config from server");
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> serverConfig = null);

    }

}
