package hotsuop.momovement.mixin;

import hotsuop.momovement.MoMovement;
import hotsuop.momovement.IMoPlayer;
import hotsuop.momovement.MoveState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DeathMessageType;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import hotsuop.momovement.api.SoundRegistry;
import hotsuop.momovement.config.MoMovementConfig;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements IMoPlayer {

    @Shadow
    public abstract boolean isMainPlayer();
    @Shadow
    protected abstract void updatePose();
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private PlayerAbilities abilities;
    @Shadow
    protected HungerManager hungerManager;

    @Unique
    private MoveState moveState = MoveState.NONE;
    @Unique
    private MoveState lastMoveState = MoveState.NONE;
    @Unique
    private Vec3d bonusVelocity = Vec3d.ZERO;
    @Unique
    private int rollTickCounter = 0;
    @Unique
    private int wallRunCounter = 0;
    @Unique
    private Vec3d lastWallDir = Vec3d.ZERO;
    @Unique
    private boolean isWallLeft = false;
    @Unique
    private int slideCooldown = 0;
    @Unique
    private int diveCooldown = 0;
    @Unique
    private BlockPos lastBlockPos = null;
    @Unique
    private boolean momovement_lastSprintingState = false;

    private static final double BONUS_DECAY_ROLLING = 0.98;
    private static final double BONUS_DECAY_DEFAULT = 0.9;
    private static final int MAX_ROLL_TICKS = 10;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public MoveState momovement_getMoveState() {
        return moveState;
    }

    @Override
    public void momovement_setMoveState(MoveState moveState) {
        this.moveState = moveState;
    }

    @Unique
    private void updateCurrentMoveState() {
        if (lastMoveState != moveState) {
            lastMoveState = moveState;
            if (moveState == MoveState.ROLLING) {
                rollTickCounter = 0;
                setPose(EntityPose.SWIMMING);
            }
            if (moveState == MoveState.PRONE) {
                rollTickCounter = 0;
                setPose(EntityPose.SWIMMING);
            }
            if (moveState == MoveState.HANGING) {
                setPose(EntityPose.STANDING);
            }
            if (this.isMainPlayer()) {
                MoMovement.moveStateUpdater.setMoveState((PlayerEntity) (Object) this, moveState);
            }
            MoMovement.moveStateUpdater.setAnimationState((PlayerEntity) (Object) this, moveState);
            updatePose();
            calculateDimensions();
        }
    }


    @Unique
    private static Vec3d momovement_movementInputToVelocity(Vec3d movementInput, double speed, float yaw) {
        double d = movementInput.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3d.ZERO;
        } else {
            Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
            double f = MathHelper.sin(yaw * 0.017453292F);
            double g = MathHelper.cos(yaw * 0.017453292F);
            return new Vec3d(vec3d.x * g - vec3d.z * f, vec3d.y, vec3d.z * g + vec3d.x * f);
        }
    }

    @Unique
    private static Vec3d momovement_velocityToMovementInput(Vec3d velocity, float yaw) {
        double d = velocity.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3d.ZERO;
        }
        float f = MathHelper.sin(yaw * 0.017453292F);
        float g = MathHelper.cos(yaw * 0.017453292F);
        Vec3d unrotatedVec = new Vec3d(
                velocity.x * g + velocity.z * f,
                velocity.y,
                -velocity.x * f + velocity.z * g
        );
        return (unrotatedVec.lengthSquared() > 1.0 ? unrotatedVec.normalize() : unrotatedVec);
    }

    @Unique
    private void momovement_WallRun() {
        Vec3d vel = getVelocity();
        boolean hasWall = getWallDirection();
    
        if (isWallRunning()) {
            continueWallRun(hasWall, vel);
        } else {
            resetWallRun();
            if (canStartWallRun(vel)) {
                startWallRun();
            }
        }
    }
    
    private boolean isWallRunning() {
        return moveState == MoveState.WALLRUNNING_LEFT || moveState == MoveState.WALLRUNNING_RIGHT;
    }
    
    private void continueWallRun(boolean hasWall, Vec3d vel) {
        if (!hasWall || isOnGround()) {
            wallRunCounter = 0;
            moveState = MoveState.NONE;
        } else {
            wallRunCounter++;
            setSprinting(true);
            updateWallRunSound();
            Vec3d wallVel = calculateWallVelocity(vel);
            adjustWallRunVelocity(wallVel, vel);
            updateVelocityForWallRun(vel);
            applyWallRunSpeedCap();
            handleWallRunExit(wallVel);
        }
    }
    
    private void updateWallRunSound() {
        BlockPos wallBlockPos = getBlockPos().subtract(BlockPos.ofFloored(lastWallDir));
        if (lastBlockPos == null || !lastBlockPos.equals(wallBlockPos)) {
            lastBlockPos = wallBlockPos;
            playStepSound(wallBlockPos, getWorld().getBlockState(wallBlockPos));
        }
    }
    
    private Vec3d calculateWallVelocity(Vec3d vel) {
        Vec3d flatVel = vel.multiply(1, 0, 1);
        return isWallLeft ? flatVel.normalize().rotateY(90) : flatVel.normalize().rotateY(-90);
    }
    
    private void adjustWallRunVelocity(Vec3d wallVel, Vec3d flatVel) {
        if (momovement_velocityToMovementInput(flatVel, getYaw()).dotProduct(lastWallDir) < 0) {
            addVelocity(wallVel.multiply(-0.1, 0, -0.1));
        }
    }
    
    private void updateVelocityForWallRun(Vec3d vel) {
        addVelocity(new Vec3d(0, -vel.y * (1 - ((double) wallRunCounter / MoMovement.getConfig().getWallRunDurationTicks())), 0));
        bonusVelocity = Vec3d.ZERO;
    }
    
    private void applyWallRunSpeedCap() {
        double maxWallRunSpeed = MoMovement.getConfig().getMaxWallRunSpeed();
        if (getVelocity().length() > maxWallRunSpeed) {
            setVelocity(getVelocity().normalize().multiply(maxWallRunSpeed));
        }
    }
    
    private void handleWallRunExit(Vec3d wallVel) {
        if (!MoMovement.INPUT.ismoveDownKeyPressed()) {
            applyWallRunBoost(wallVel);
            moveState = MoveState.NONE;
        }
    }
    
    private void applyWallRunBoost(Vec3d wallVel) {
        double velocityMult = MoMovement.getConfig().getWallRunSpeedBoostMultiplier();
        Vec3d boost = wallVel.multiply(0.3 * velocityMult, 0, 0.3 * velocityMult).add(new Vec3d(0, 0.4 * velocityMult, 0));
        double maxBoost = 0.5;
        if (boost.length() > maxBoost) {
            boost = boost.normalize().multiply(maxBoost);
        }
        addVelocity(boost);
    }
    
    private void resetWallRun() {
        wallRunCounter = 0;
    }
    
    private boolean canStartWallRun(Vec3d vel) {
        return !isOnGround() && MoMovement.INPUT.ismoveDownKeyPressed() && getWallDirection() && vel.y <= 0;
    }
    
    private void startWallRun() {
        playWallRunSound();
        moveState = isWallLeft ? MoveState.WALLRUNNING_RIGHT : MoveState.WALLRUNNING_LEFT;
        hungerManager.addExhaustion(MoMovement.getConfig().getWallRunStaminaCost());
    }
    
    private void playWallRunSound() {
        playSound(SoundRegistry.MY_SOUND_EVENT, 0.5f, 1.0f);
    }
    
    @Unique
    private boolean getWallDirection() {
        Vec3d flat = getVelocity().multiply(1, 0, 1);
        if (flat.lengthSquared() < 0.01) return false;
        flat = flat.normalize();
        World world = getWorld();
        Vec3d left = flat.rotateY(-90).multiply(0.5, 0, 0.5);
        Vec3d right = flat.rotateY(90).multiply(0.5, 0, 0.5);
    
        if (isWallInDirection(left, world)) {
            lastWallDir = getBlockPos().toCenterPos().subtract(world.raycast(new RaycastContext(getPos().add(0, 0.2, 0), getPos().add(left), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getBlockPos().toCenterPos());
            isWallLeft = true;
            return true;
        }
        if (isWallInDirection(right, world)) {
            lastWallDir = getBlockPos().toCenterPos().subtract(world.raycast(new RaycastContext(getPos().add(0, 0.2, 0), getPos().add(right), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getBlockPos().toCenterPos());
            isWallLeft = false;
            return true;
        }
        lastWallDir = Vec3d.ZERO;
        return false;
    }
    
    private boolean isWallInDirection(Vec3d direction, World world) {
        var lowerHit = world.raycast(new RaycastContext(getPos().add(0, 0.2, 0), getPos().add(direction), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        if (lowerHit.getType() == HitResult.Type.BLOCK) {
            var upperHit = world.raycast(new RaycastContext(getPos().add(0, 1.5, 0), getPos().add(direction), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
            if (upperHit.getType() == HitResult.Type.BLOCK) {
                return true;
            }
        }
        return false;
    }
    
    

    @Unique
    private boolean momovement_isValidForMovement(boolean canSwim, boolean canElytra) {
        return !isSpectator() && (canElytra || !isFallFlying()) && (canSwim || !isTouchingWater()) && !isClimbing() && !abilities.flying;
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    public void momovement_getDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        MoveState currentState = momovement_getMoveState();
        if (currentState != null && currentState != MoveState.NONE) cir.setReturnValue(currentState.dimensions);
    }

    @Inject(method = "getActiveEyeHeight", at = @At("HEAD"), cancellable = true)
    public void momovement_getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        MoveState currentState = momovement_getMoveState();
        if (currentState != null && currentState != MoveState.NONE) cir.setReturnValue(currentState.dimensions.height * 0.85f);
    }
  private int vaultStep = 0;
private final int totalVaultSteps = 10; // Steps for detailed vault
private final double vaultHeight = 1.25; // Height to vault (slightly higher than a block)
private final double forwardDistance = 1.0; // Forward distance to vault
private boolean isVaulting = false;
@Unique
private void startVaulting() {
    if (moveState != MoveState.VAULTING) {
        moveState = MoveState.VAULTING;
        momovement_movementInputToVelocity(new Vec3d(0, 0, 1), 0.1f, getYaw());
        setSprinting(true);
        vaultStep = 0;
        isVaulting = true;

        LOGGER.info("Vaulting started");
    }
}

@Unique
private void handleVaulting() {
    if (isVaulting && moveState == MoveState.VAULTING) {
        double yOffset = 0;
        double forwardOffset = 0;

        // Three phases: rapid ascent, forward movement, rapid descent
        if (vaultStep < totalVaultSteps / 3) {
            // Rapid ascent
            yOffset = vaultHeight * (3.0 * vaultStep / totalVaultSteps);
        } else if (vaultStep < 2 * totalVaultSteps / 3) {
            // Forward movement at peak height
            yOffset = vaultHeight;
            forwardOffset = forwardDistance * ((3.0 * vaultStep / totalVaultSteps) - 1);
        } else {
            // Rapid descent
            yOffset = vaultHeight * (3.0 * (totalVaultSteps - vaultStep) / totalVaultSteps);
            forwardOffset = forwardDistance;
        }

        Vec3d currentPos = getPos();
        double yaw = Math.toRadians(getYaw());
        double xOffset = forwardOffset * Math.sin(yaw);
        double zOffset = forwardOffset * Math.cos(yaw);

        Vec3d stepVelocity = new Vec3d(xOffset / totalVaultSteps, yOffset / totalVaultSteps, zOffset / totalVaultSteps);

        setVelocity(stepVelocity);
        setPos(currentPos.x + stepVelocity.x, currentPos.y + yOffset / totalVaultSteps, currentPos.z + stepVelocity.z);

        vaultStep++;

        if (vaultStep >= totalVaultSteps) {
            moveState = MoveState.NONE;
            isVaulting = false;
            LOGGER.info("Vaulting handled");
        }
    }
}

@Inject(method = "tick", at = @At("HEAD"))
private void momovement_tick(CallbackInfo info) {
    if (!MoMovement.getConfig().enableMoMovement) return;

    if (this.isMainPlayer()) {
        if (abilities.flying || getControllingVehicle() != null) {
            moveState = MoveState.NONE;
            updateCurrentMoveState();
            return;
        }

        if (moveState == MoveState.ROLLING) {
            handleRolling();
        }

        if (moveState == MoveState.SLIDING && !MoMovement.INPUT.ismoveDownKeyPressed()) {
            moveState = MoveState.NONE;
        }

        if (MoMovement.getConfig().wallRunEnabled) momovement_WallRun();
        if (MoMovement.getConfig().ledgeGrabEnabled) momovement_LedgeGrab();
        if (MoMovement.getConfig().vaultingEnabled && canVault() && MoMovement.INPUT.ismoveUpKeyPressed()) {
            startVaulting();
        }

        addVelocity(bonusVelocity);
        bonusVelocity = bonusVelocity.multiply(getBonusDecay(), 0, getBonusDecay());
    }

    updateCurrentMoveState();
}

@Inject(method = "travel", at = @At("HEAD"))
private void momovement_travel(Vec3d movementInput, CallbackInfo info) {
    if (!isMainPlayer() || !MoMovement.getConfig().enableMoMovement || abilities.flying || getControllingVehicle() != null)
        return;

    momovement_lastSprintingState = isSprinting();

    if (moveState == MoveState.VAULTING) {
        handleVaulting();
        return;
    }

    if (MoMovement.INPUT.ismoveDownKeyPressed() && !MoMovement.INPUT.ismoveDownKeyPressedLastTick()) {
        handleSpecialMovements();
    } else if (moveState == MoveState.PRONE) {
        moveState = MoveState.NONE;
    }
}


    private void handleRolling() {
        rollTickCounter++;
        if (rollTickCounter >= MAX_ROLL_TICKS) {
            rollTickCounter = 0;
            moveState = MoMovement.INPUT.ismoveDownKeyPressed() ? MoveState.PRONE : MoveState.NONE;
        }
    }

    private double getBonusDecay() {
        return moveState == MoveState.ROLLING ? BONUS_DECAY_ROLLING : BONUS_DECAY_DEFAULT;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void momovement_tick_tail(CallbackInfo info) {
        if (!MoMovement.getConfig().enableMoMovement) return;
        if (moveState == MoveState.PRONE || moveState == MoveState.ROLLING) setPose(EntityPose.SWIMMING);
        if (diveCooldown > 0) diveCooldown--;
        if (slideCooldown > 0) slideCooldown--;
    }

    
    
@Unique
private boolean canVault() {
    BlockPos playerPos = new BlockPos(MathHelper.floor(getX()), MathHelper.floor(getY()), MathHelper.floor(getZ()));
    World world = getEntityWorld();
    BlockPos blockInFrontPos = playerPos.offset(getHorizontalFacing());
    BlockPos blockAboveInFrontPos = blockInFrontPos.up();
    BlockState blockInFront = world.getBlockState(blockInFrontPos);
    BlockState blockAboveInFront = world.getBlockState(blockAboveInFrontPos);

    boolean vaultable = blockInFront.isSolidBlock(world, blockInFrontPos) && !blockAboveInFront.isSolidBlock(world, blockAboveInFrontPos);
    LOGGER.info("Vaultable: " + vaultable);
    return vaultable;
}




    
    private void handleSpecialMovements() {
        var conf = MoMovement.getConfig();

        boolean canDiveRoll = canPerformDiveRoll(conf);
        boolean canSlide = canPerformSlide(conf);

        if (canDiveRoll) {
            performDiveRoll(conf);
        } else if (canSlide) {
            performSlide(conf);
        }
    }

    private boolean canPerformDiveRoll(MoMovementConfig conf) {
        return diveCooldown == 0 && conf.diveRollEnabled && !isOnGround()
                && getVelocity().multiply(1, 0, 1).lengthSquared() > 0.05
                && momovement_isValidForMovement(conf.diveRollWhenSwimming, conf.diveRollWhenFlying);
    }

    private boolean canPerformSlide(MoMovementConfig conf) {
        return slideCooldown == 0 && conf.slideEnabled && momovement_lastSprintingState
                && momovement_isValidForMovement(false, false);
    }

    private void performDiveRoll(MoMovementConfig conf) {
        diveCooldown = conf.getDiveRollCoolDown() / 2;
        hungerManager.addExhaustion(conf.getDiveRollStaminaCost());
        moveState = MoveState.ROLLING;
        bonusVelocity = momovement_movementInputToVelocity(Vec3d.ZERO, 0.15f * conf.getDiveRollSpeedBoostMultiplier(), getYaw());
        setSprinting(true);
        playFeedback();
    }

    private void performSlide(MoMovementConfig conf) {
        slideCooldown = conf.getSlideCoolDown() / 2;
        hungerManager.addExhaustion(conf.getSlideStaminaCost());
        moveState = MoveState.SLIDING;
        bonusVelocity = momovement_movementInputToVelocity(new Vec3d(0, 0, 1), 0.25f * conf.getSlideSpeedBoostMultiplier(), getYaw());
        setSprinting(true);
        playFeedback();
    }

    private void playFeedback() {
        // TODO: Implement sound feedback
    }

    @Inject(method = "adjustMovementForSneaking", at = @At("HEAD"), cancellable = true)
    private void momovement_adjustMovementForSneaking(Vec3d movement, MovementType type, CallbackInfoReturnable<Vec3d> cir) {
        if (this.isMainPlayer() && (moveState == MoveState.ROLLING || moveState == MoveState.SLIDING)) {
            cir.setReturnValue(movement);
        }
    }

    @Inject(method = "jump", at = @At("HEAD"))
    private void momovement_jump(CallbackInfo info) {
        if (this.isMainPlayer()) {
            setSprinting(momovement_lastSprintingState);
            if (moveState == MoveState.SLIDING || moveState == MoveState.PRONE) {
                moveState = MoveState.NONE;
            }
            if (moveState == MoveState.ROLLING) {
                moveState = MoveState.PRONE;
            }
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void momovement_damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getType().deathMessageType() == DeathMessageType.FALL_VARIANTS && moveState == MoveState.ROLLING) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Unique
    private boolean canHangUnderBlock() {
        BlockPos playerPos = new BlockPos(MathHelper.floor(getX()), MathHelper.floor(getY()), MathHelper.floor(getZ()));
        World world = getEntityWorld();
        BlockPos blockAbovePos = playerPos.up();
        BlockState blockAbove = world.getBlockState(blockAbovePos);

        return blockAbove.isSolidBlock(world, blockAbovePos)
                || blockAbove.isIn(BlockTags.CLIMBABLE)
                || isFence(blockAbove);
    }

    private boolean isFence(BlockState state) {
        Block block = state.getBlock();
        return block instanceof FenceBlock || block instanceof FenceGateBlock;
    }

    @Unique
    private void handleClimbing() {
        if (canHangUnderBlock()) {
            if (moveState != MoveState.HANGING) {
                moveState = MoveState.HANGING;
                setPose(EntityPose.STANDING);
            }

            adjustHangingVelocity();

            if (MoMovement.INPUT.ismoveUpKeyPressed()) {
                setVelocity(getVelocity().add(0, 2, 0));
            } else if (MoMovement.INPUT.ismoveDownKeyPressed()) {
                setVelocity(getVelocity().add(0, -1, 0));
            }
        } else if (moveState == MoveState.HANGING) {
            moveState = MoveState.NONE;
        }
    }

    private void adjustHangingVelocity() {
        setVelocity(getVelocity().multiply(0.5, 3, 0.5));
    }

    @Unique
    private void momovement_LedgeGrab() {
        if (moveState == MoveState.LEDGE_GRABBING) {
            handleLedgeHanging();
        } else if (canInitiateLedgeGrab()) {
            initiateLedgeGrab();
        }
    }

    private void handleLedgeHanging() {
        if (MoMovement.INPUT.ismoveUpKeyPressed()) {
            climbLedge();
        } else if (!isLedgeGrabbingConditionsMet()) {
            moveState = MoveState.NONE;
            setVelocity(new Vec3d(getVelocity().x, +0.5, getVelocity().z));
        } else {
            setVelocity(Vec3d.ZERO);
            setPos(getX(), Math.floor(getY()), getZ());
        }
    }

    private void climbLedge() {
        moveState = MoveState.NONE;
        // Add climb logic here
    }

    private boolean canInitiateLedgeGrab() {
        return !isOnGround() && getVelocity().y < 0 && canGrabLedge() && MoMovement.INPUT.ismoveDownKeyPressed();
    }

    private void initiateLedgeGrab() {
        moveState = MoveState.LEDGE_GRABBING;
        // Add initiation logic here
    }

    private boolean isLedgeGrabbingConditionsMet() {
        return !isOnGround() && canGrabLedge();
    }

    @Unique
    private boolean canGrabLedge() {
        BlockPos playerPos = new BlockPos(MathHelper.floor(getX()), MathHelper.floor(getY()), MathHelper.floor(getZ()));
        World world = getEntityWorld();
        BlockPos blockInFrontPos = playerPos.offset(getHorizontalFacing());
        BlockPos blockAboveInFrontPos = blockInFrontPos.up();
        BlockState blockInFront = world.getBlockState(blockInFrontPos);
        BlockState blockAboveInFront = world.getBlockState(blockAboveInFrontPos);

        return blockInFront.isSolidBlock(world, blockInFrontPos) && !blockAboveInFront.isSolidBlock(world, blockAboveInFrontPos);
    }
}
