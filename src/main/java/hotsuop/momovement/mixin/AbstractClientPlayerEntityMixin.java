package hotsuop.momovement.mixin;

import com.mojang.authlib.GameProfile;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import hotsuop.momovement.IAnimatedPlayer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin implements IAnimatedPlayer{
    @Unique
    private final ModifierLayer<IAnimation> mainAnimationLayer = new ModifierLayer<>();
    @Unique
    private final ModifierLayer<IAnimation> bodyAnimationLayer = new ModifierLayer<>();

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void init(ClientWorld world, GameProfile profile, CallbackInfo ci) {
        PlayerAnimationAccess.getPlayerAnimLayer((AbstractClientPlayerEntity) (Object)this).addAnimLayer(1, mainAnimationLayer);
        PlayerAnimationAccess.getPlayerAnimLayer((AbstractClientPlayerEntity) (Object)this).addAnimLayer(9999, bodyAnimationLayer);
    }

    @Override
    public ModifierLayer<IAnimation> momovement_getModAnimation() {
        return mainAnimationLayer;
    }

    @Override
    public ModifierLayer<IAnimation> momovement_getModAnimationBody() {
        return bodyAnimationLayer;
    }

}
