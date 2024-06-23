package hotsuop.momovement;

public interface IMoPlayer {
    MoveState momovement_getMoveState();
    void momovement_setMoveState(MoveState moveState);
    void momovement_setJumpInput(boolean input);
    boolean momovement_hasDoubleJumped();
    void momovement_setDoubleJumped(boolean doubleJumped);
    int momovement_getDoubleJumpCooldown();
    void momovement_setDoubleJumpCooldown(int cooldownTicks);
}
