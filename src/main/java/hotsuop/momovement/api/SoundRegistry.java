package hotsuop.momovement.api;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;


public class SoundRegistry{


    public static final Identifier MY_SOUND_ID = new Identifier("momovement:wallrun_running");
    public static SoundEvent MY_SOUND_EVENT = SoundEvent.of(MY_SOUND_ID);

    public static void registerSound() {
        Registry.register(Registries.SOUND_EVENT, MY_SOUND_ID, MY_SOUND_EVENT);
    }

}
