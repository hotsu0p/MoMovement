package hotsuop.momovement.config;

import net.minecraft.client.gui.screen.Screen;
import net.uku3lig.ukulib.config.ConfigManager;
import net.uku3lig.ukulib.config.option.*;
import net.uku3lig.ukulib.config.screen.AbstractConfigScreen;

public class MoMovementConfigScreen extends AbstractConfigScreen<MoMovementConfig> {



    protected MoMovementConfigScreen(Screen parent, ConfigManager<MoMovementConfig> manager) {
        super("MoMovement Config", parent, manager);
    }


    @Override
    protected WidgetCreator[] getWidgets(MoMovementConfig config) {
        return new WidgetCreator[]{
                CyclingOption.ofBoolean("text.config.momovement.option.enableMoMovement", config.enableMoMovement,  config::setEnableMoMovement),

                new TextOption("text.config.momovement.category.diveRoll", true).wide(),
                CyclingOption.ofBoolean("text.config.momovement.option.diveRoll.enabled", config.diveRollEnabled, config::setDiveRollEnabled).wide(),
                new IntSliderOption("text.config.momovement.option.diveRoll.staminaCost", config.getDiveRollStaminaCost(), config::setDiveRollStaminaCost, IntSliderOption.DEFAULT_INT_TO_TEXT,0,256),
                new IntSliderOption("text.config.momovement.option.diveRoll.cooldown", config.getDiveRollCoolDown(), config::setDiveRollCoolDown, IntSliderOption.DEFAULT_INT_TO_TEXT,0,256),
                new SliderOption("text.config.momovement.option.diveRoll.speedBoostMult", config.getDiveRollSpeedBoostMultiplier(), config::setDiveRollSpeedBoostMultiplier, SliderOption.PERCENT_VALUE_TO_TEXT,0,3),
                CyclingOption.ofBoolean("text.config.momovement.option.diveRoll.whenSwimming", config.diveRollWhenSwimming, config::setDiveRollWhenSwimming),
                CyclingOption.ofBoolean("text.config.momovement.option.diveRoll.whenFlying", config.diveRollWhenFlying, config::setDiveRollWhenFlying),

                new TextOption("text.config.momovement.category.slide", true).wide(),
                CyclingOption.ofBoolean("text.config.momovement.option.slide.enabled", config.slideEnabled, config::setSlideEnabled).wide(),
                new IntSliderOption("text.config.momovement.option.slide.staminaCost", config.getSlideStaminaCost(), config::setSlideStaminaCost, IntSliderOption.DEFAULT_INT_TO_TEXT,0,256),
                new IntSliderOption("text.config.momovement.option.slide.cooldown", config.getSlideCoolDown(), config::setSlideCoolDown, IntSliderOption.DEFAULT_INT_TO_TEXT,0,256),
                new SliderOption("text.config.momovement.option.slide.speedBoostMult", config.getSlideSpeedBoostMultiplier(), config::setSlideSpeedBoostMultiplier, SliderOption.PERCENT_VALUE_TO_TEXT,0,3),

                new TextOption("text.config.momovement.category.wallRun", true).wide(),
                CyclingOption.ofBoolean("text.config.momovement.option.wallRun.enabled", config.wallRunEnabled, config::setWallRunEnabled).wide(),
                new IntSliderOption("text.config.momovement.option.wallRun.staminaCost", config.getWallRunStaminaCost(), config::setWallRunStaminaCost, IntSliderOption.DEFAULT_INT_TO_TEXT,0,256),
                new IntSliderOption("text.config.momovement.option.wallRun.durationTicks", config.getWallRunDurationTicks(), config::setWallRunDurationTicks, IntSliderOption.DEFAULT_INT_TO_TEXT,1,256),
                new SliderOption("text.config.momovement.option.wallRun.speedBoostMult", config.getWallRunSpeedBoostMultiplier(), config::setWallRunSpeedBoostMultiplier, SliderOption.PERCENT_VALUE_TO_TEXT,0,3),
        };
    }


}
