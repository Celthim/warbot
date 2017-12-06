package PF;

import edu.warbot.agents.MovableWarAgent;

import edu.warbot.agents.agents.WarExplorer;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.agents.resources.WarFood;
import edu.warbot.brains.WarBrain;
import edu.warbot.brains.brains.WarExplorerBrain;
import edu.warbot.communications.WarMessage;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public abstract class WarExplorerBrainController extends WarExplorerBrain {

    WTask ctask;
    
    public List<WarAgentPercept> getPerceptsRessources(){
        List<WarAgentPercept> ressources = new ArrayList<>();
        for( WarAgentPercept wap : getPercepts())
            if (wap.getType() == WarAgentType.WarFood)
                ressources.add(wap);
        return ressources;
    }
    
    static WTask scoutForEnemyBase = new WTask(){
        String exec(WarBrain bc){
            WarExplorerBrainController me = (WarExplorerBrainController) bc;

            if(!me.getPerceptsEnemiesByType(WarAgentType.WarBase).isEmpty()){
                me.ctask=callForEnemyBase;
                return null;
            }
            return VUtils.Wiggle(me);
        }
        
    };
    
    static WTask callForEnemyBase = new WTask(){
        String exec(WarBrain bc){
            WarExplorerBrainController me = (WarExplorerBrainController) bc;
            WarAgentPercept wp = me.getPerceptsEnemiesByType(WarAgentType.WarBase).get(0);
            String[] s = {""+wp.getDistance(),""+wp.getAngle()};
            me.broadcastMessageToAgentType(WarAgentType.WarRocketLauncher, "CFP attack enemyBase", s);
            return ACTION_IDLE;
        }
        
    };

    static WTask returnFoodTask = new WTask() {
        String exec(WarBrain bc) {
            WarExplorerBrainController me = (WarExplorerBrainController) bc;
            if (me.isBagEmpty()) {
                me.setHeading(me.getHeading() + 180);

                me.ctask = getFoodTask;
                return (null);
            }

            //me.setDebugStringColor(Color.green.darker());
            VUtils.Fier(me);
            me.setDebugString("Returning Food");

            if (me.isBlocked()) {
                me.setRandomHeading();
            }

            ArrayList<WarAgentPercept> basePercepts = (ArrayList<WarAgentPercept>) me.getPerceptsAlliesByType(WarAgentType.WarBase);

            //Si je ne vois pas de base
            if (basePercepts == null | basePercepts.size() == 0) {

                WarMessage m = me.getMessageFromBase();
                //Si j'ai un message de la base je vais vers elle
                if (m != null) {
                    me.setHeading(m.getAngle());
                }

                //j'envoie un message aux bases pour savoir où elle sont..
                me.broadcastMessageToAgentType(WarAgentType.WarBase, "Where are you?", (String[]) null);

                return (MovableWarAgent.ACTION_MOVE);

            } else {//si je vois une base
                WarAgentPercept base = basePercepts.get(0);

                if (base.getDistance() > MovableWarAgent.MAX_DISTANCE_GIVE) {
                    me.setHeading(base.getAngle());
                    return (MovableWarAgent.ACTION_MOVE);
                } else {
                    me.setIdNextAgentToGive(base.getID());
                    return (MovableWarAgent.ACTION_GIVE);
                }

            }

        }
    };

    static WTask getFoodTask = new WTask() {
        String exec(WarBrain bc) {
            WarExplorerBrainController me = (WarExplorerBrainController) bc;
            if (me.isBagFull()) {

                me.ctask = returnFoodTask;
                return (null);
            }
            
            if (me.isBlocked()) {
                me.setRandomHeading();
            }

            me.setDebugStringColor(Color.BLACK);
            me.setDebugString("Searching food");

            WarAgentPercept closest;
            ArrayList<WarAgentPercept> food = (ArrayList<WarAgentPercept>) me.getPerceptsRessources();
            if (!food.isEmpty()) {
                closest = null;
                for (WarAgentPercept wap : food) {                        
                        me.setDebugString("Found Food");
                        me.broadcastMessageToAgentType(WarAgentType.WarExplorer, "foodHere", (String[]) null);
                        if (wap.getDistance() <= WarFood.MAX_DISTANCE_TAKE) {
                            return (MovableWarAgent.ACTION_TAKE);
                        } else if (closest == null || wap.getDistance() < closest.getDistance()) {
                            closest = wap;
                        }
                }
                if (closest != null) {
                    me.setHeading(closest.getAngle());
                }
            } 
            else {
                WarMessage message = me.getMessageAboutFood();
                if(message!=null)
                    me.setHeading(message.getAngle());
            }
            return VUtils.Wiggle(me);
        }
    };

    public WarExplorerBrainController() {
        super();
        ctask = scoutForEnemyBase; // initialisation de la FSM
    }

    @Override
    public String action() {
        
                

        // Develop behaviour here
        String toReturn = ctask.exec(this);   // le run de la FSM

        if (toReturn == null) {
            if (isBlocked()) {
                setRandomHeading();
            }
            return WarExplorer.ACTION_MOVE;
        } else {
            return toReturn;
        }
    }

    private WarMessage getMessageAboutFood() {
        for (WarMessage m : getMessages()) {
            if (m.getMessage().equals("foodHere")) {
                String s = m.getContent()[0];
                return m;                
            }
        }
        return null;
    }

    private WarMessage getMessageFromBase() {
        for (WarMessage m : getMessages()) {
            if (m.getSenderType().equals(WarAgentType.WarBase)) {
                return m;
            }
        }
        return null;
    }
}
