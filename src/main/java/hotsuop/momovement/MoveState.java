package hotsuop.momovement;

import net.minecraft.entity.EntityDimensions;
import java.util.Map;

public class MoveState {

    public static final MoveState NONE = new MoveState("none", false);
    public static final MoveState SLIDING = new MoveState("sliding", true);
    public static final MoveState ROLLING = new MoveState("rolling", true);
    public static final MoveState WALLRUNNING_LEFT = new MoveState("wallrunning_left", false);
    public static final MoveState WALLRUNNING_RIGHT = new MoveState("wallrunning_right", false);
    public static final MoveState VAULTING = new MoveState("vaulting", false);
    public static final MoveState PRONE = new MoveState("prone", true);
    public static final MoveState CLIMBING = new MoveState("climbing", false);
    public static final MoveState HANGING = new MoveState("hanging", false);
    public static final MoveState LEDGE_GRABBING = new MoveState("ledge_grabbing", false);

    public static final Map<Integer,MoveState> STATES = Map.of(
        // these are all the availbe move states
            0, NONE,
            1, SLIDING,
            2, PRONE,
            3, ROLLING,
            4, WALLRUNNING_LEFT,
            5, WALLRUNNING_RIGHT,
            6, VAULTING,
            7, CLIMBING,
            8, HANGING,
            9, LEDGE_GRABBING
    );

    public static MoveState STATE(int id){
        return STATES.getOrDefault(id, NONE);
    }

    public static int STATE(MoveState state){
        for(Map.Entry<Integer, MoveState> entry : STATES.entrySet()){
            if(entry.getValue().equals(state)){
                return entry.getKey();
            }
        }
        return 0;
    }

    public String name;
    public EntityDimensions dimensions;
    public MoveState(String name, boolean isSmall){
        this.name = name;
        this.dimensions = EntityDimensions.changing(0.6F, isSmall ? 0.8F : 1.8F);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof MoveState){
            return ((MoveState) obj).name.equals(this.name);
        }
        if(obj instanceof String){
            return ((String) obj).equals(this.name);
        }
        if(obj == null && this.name.equals("none")){
            return true;
        }

        return false;
    }

}
