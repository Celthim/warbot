package PF;

import static edu.warbot.agents.actions.constants.AgressiveActions.ACTION_FIRE;
import static edu.warbot.agents.actions.constants.AgressiveActions.ACTION_RELOAD;
import static edu.warbot.agents.actions.constants.IdlerActions.ACTION_IDLE;
import edu.warbot.agents.agents.WarRocketLauncher;
import edu.warbot.agents.projectiles.WarRocket;
import edu.warbot.brains.WarBrain;
import edu.warbot.brains.brains.WarRocketLauncherBrain;
import edu.warbot.communications.WarMessage;

public abstract class WarRocketLauncherBrainController extends WarRocketLauncherBrain {
    
    WTask ctask;
    Double[] target;
    
    static WTask idle = new WTask() {
        String exec(WarBrain bc) {
            return null;
        }
    };
     

    static WTask attackEnemyBase = new WTask() {
        String exec(WarBrain bc) {
            WarRocketLauncherBrainController me = (WarRocketLauncherBrainController) bc;
            me.setHeading(me.target[1]);
            if(me.target[0] > WarRocket.RANGE){
                me.target[0]-=WarRocketLauncher.SPEED;
                return WarRocketLauncherBrain.ACTION_MOVE;
            } else {
                if (me.isReloaded())
                    return ACTION_FIRE;
                else if (me.isReloading())
                    return ACTION_IDLE;
                else
                    return ACTION_RELOAD;
            }
        }
    };

    public WarRocketLauncherBrainController() {
        super();
        this.ctask=idle;
    }

    @Override
    public String action() {
        
        for(WarMessage m : getMessages()){
            String[] message = VUtils.decodeMessage(m);
            this.setDebugString(message[0]+message[1]+message[2]);
            switch(message[0]){
                case "CFP" :
                    switch(message[1]){
                        case "attack" :
                            switch(message[2]){
                                case "enemyBase" ://CFP attack enemyBase
                                    if(this.ctask!=attackEnemyBase)
                                    this.target=Vector2.addPolars(m.getDistance(), m.getAngle(), Double.parseDouble(m.getContent()[0]), Double.parseDouble(m.getContent()[1]));
                                    this.ctask=attackEnemyBase;
                                    
                                break;
                            }
                            break;
                    }
                    break;
                default : ;
            }
        }

        // Develop behaviour here
        String toReturn = ctask.exec(this);   // le run de la FSM

        if (toReturn == null) {
            if (isBlocked()) {
                setRandomHeading();
            }
            return WarRocketLauncherBrain.ACTION_MOVE;
        } else {
            return toReturn;
        }       
    }
       
}
