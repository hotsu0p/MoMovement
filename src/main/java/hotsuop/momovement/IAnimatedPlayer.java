package hotsuop.momovement;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;

public interface IAnimatedPlayer {

    ModifierLayer<IAnimation> momovement_getModAnimation();
    ModifierLayer<IAnimation> momovement_getModAnimationBody();


}
