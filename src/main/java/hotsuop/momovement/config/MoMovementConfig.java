package hotsuop.momovement.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
public class MoMovementConfig implements Serializable {
    public MoMovementConfig() {
        enableMoMovement = true;
        diveRollEnabled = true;
        diveRollStaminaCost = 50;
        diveRollSpeedBoostMultiplier = 1.1;
        climbingEnabled = true; // Default value for climbing enabled
        climbingSpeed = 0.0002;   
        diveRollCoolDown = 0;
        diveRollWhenSwimming = false;
        diveRollWhenFlying = false;
        wallRunEnabled = true;
        wallRunStaminaCost = 0;
        wallRunSpeedBoostMultiplier = 1.1;
        wallRunDurationTicks = 60;
        maxWallRunSpeed = 0.5; 
        slideEnabled = true;
        slideStaminaCost = 10;
        slideSpeedBoostMultiplier = .5;
        slideCoolDown = 0;
        ledgeGrabEnabled = true;
        doubleJumpVelocity = 0.5;
       vaultingEnabled = true; 
    }

    public boolean enableMoMovement;
    public boolean climbingEnabled;
    public boolean ledgeGrabEnabled;
    public double climbingSpeed;
    public boolean diveRollEnabled;
    public boolean vaultingEnabled;
    public int diveRollStaminaCost;
    public double diveRollSpeedBoostMultiplier;
    public int diveRollCoolDown;
    public boolean diveRollWhenSwimming;
    public boolean diveRollWhenFlying;
    public double doubleJumpVelocity;
    public boolean wallRunEnabled;
    public int wallRunStaminaCost;
    public double wallRunSpeedBoostMultiplier;
    public int wallRunDurationTicks;
    public double maxWallRunSpeed;
    public boolean slideEnabled;
    public int slideStaminaCost;
    public double slideSpeedBoostMultiplier;
    public int slideCoolDown;

    public MoMovementConfig(MoMovementConfig config) {
        enableMoMovement = config.enableMoMovement;
        diveRollEnabled = config.diveRollEnabled;
        diveRollStaminaCost = config.diveRollStaminaCost;
        diveRollSpeedBoostMultiplier = config.diveRollSpeedBoostMultiplier;
        diveRollCoolDown = config.diveRollCoolDown;
        diveRollWhenSwimming = config.diveRollWhenSwimming;
        diveRollWhenFlying = config.diveRollWhenFlying;
        wallRunEnabled = config.wallRunEnabled;
        wallRunStaminaCost = config.wallRunStaminaCost;
        wallRunSpeedBoostMultiplier = config.wallRunSpeedBoostMultiplier;
        wallRunDurationTicks = config.wallRunDurationTicks;
        maxWallRunSpeed = config.maxWallRunSpeed; 
        slideEnabled = config.slideEnabled;
        slideStaminaCost = config.slideStaminaCost;
        slideSpeedBoostMultiplier = config.slideSpeedBoostMultiplier;
        slideCoolDown = config.slideCoolDown;
        ledgeGrabEnabled = config.ledgeGrabEnabled;
        climbingEnabled = config.climbingEnabled;
        climbingSpeed = config.climbingSpeed;
        doubleJumpVelocity = config.doubleJumpVelocity;
        vaultingEnabled = config.vaultingEnabled;
    }
}
