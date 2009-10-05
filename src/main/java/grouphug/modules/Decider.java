package grouphug.modules;

import grouphug.Grouphug;
import grouphug.GrouphugModule;

import java.util.Random;

public class Decider implements GrouphugModule {

    private static final String TRIGGER = "decide ";
    private static final String TRIGGER_HELP = "decide";
    private Random random = new Random(System.currentTimeMillis());

    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(TRIGGER))
            return;

        message = message.substring(TRIGGER.length());

        String[] choices = message.split(" ");

        Grouphug.getInstance().sendMessage("The roll of the dice picks: "+choices[random.nextInt(choices.length)], false);
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // no special action
    }

    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return TRIGGER_HELP;
    }

    public String helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_HELP)) {
            return "Decider: Helps you make tough decisions.\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER+"<choice 1> <choice 2> <choice n...>\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER+"homework wow";
        }
        return null;
    }
}
