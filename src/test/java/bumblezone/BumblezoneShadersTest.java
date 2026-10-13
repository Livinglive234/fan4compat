import dev.fan4.compat.bumblezone.BumblezoneShaders;
import java.util.*;

public final class BumblezoneShadersTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static final class Fake implements BumblezoneShaders.Host {
        boolean on=true;final List<String> messages=new ArrayList<>();int changes;
        public boolean shadersOn(){return on;}
        public void setShaders(boolean value){on=value;changes++;}
        public void message(String text){messages.add(text);}
    }
    static final String BZ=BumblezoneShaders.DIMENSION,OVERWORLD="minecraft:overworld",NETHER="minecraft:the_nether";
    public static void main(String[] args) {
        // Entering turns shaders off once, with the message; staying (ticks repeating the dimension) does nothing more.
        Fake host=new Fake();BumblezoneShaders logic=new BumblezoneShaders();
        logic.dimension(OVERWORLD,false,host);check(host.on&&host.changes==0,"the overworld leaves shaders alone");
        logic.dimension(BZ,false,host);
        check(!host.on&&host.messages.equals(List.of("The Bumblezone is best experienced without shaders")),"entering turns shaders off and says so");
        for(int i=0;i<5;i++)logic.dimension(BZ,false,host);
        check(host.changes==1&&host.messages.size()==1,"staying does nothing more");

        // A TARDIS does not count, in either direction; shaders stay off.
        logic.dimension("drwho:tardis_1",true,host);check(!host.on,"a TARDIS inside the Bumblezone keeps them off");
        logic.dimension(BZ,false,host);check(!host.on&&host.changes==1&&host.messages.size()==1,"coming back out of the TARDIS into the Bumblezone is not a new entry");
        logic.dimension("drwho:tardis_1",true,host);logic.dimension("drwho:tardis_2",true,host);check(!host.on,"TARDIS to TARDIS changes nothing");
        // Leaving for a real dimension, even by way of a TARDIS, brings them back once.
        logic.dimension(OVERWORLD,false,host);check(host.on&&host.changes==2,"another dimension turns shaders back on");
        logic.dimension(NETHER,false,host);check(host.changes==2,"and nothing more after that");
        logic.dimension(BZ,false,host);check(!host.on&&host.messages.size()==2,"the next visit turns them off and says so again");

        // Shaders the player had off are not turned on, and say nothing.
        Fake off=new Fake();off.on=false;BumblezoneShaders quiet=new BumblezoneShaders();
        quiet.dimension(BZ,false,off);quiet.dimension(OVERWORLD,false,off);
        check(!off.on&&off.changes==0&&off.messages.isEmpty(),"shaders that were already off are not touched, and there is no message");
        // Turned back on by hand inside: left alone on the way out, and not turned off again until the next entry.
        Fake manual=new Fake();BumblezoneShaders hand=new BumblezoneShaders();
        hand.dimension(BZ,false,manual);manual.on=true;hand.dimension(BZ,false,manual);
        check(manual.on&&manual.messages.size()==1,"shaders turned on again by hand inside stay on");
        hand.dimension(OVERWORLD,false,manual);check(manual.on&&manual.changes==1,"and leaving does not switch anything");
        // Leaving the world gives the shaders back; entering the world already inside the Bumblezone turns them off.
        Fake world=new Fake();BumblezoneShaders session=new BumblezoneShaders();
        session.dimension(BZ,false,world);session.leftWorld(world);check(world.on,"leaving the world restores the shaders");
        session.leftWorld(world);check(world.changes==2,"and does so only once");
        session.dimension(BZ,false,world);check(!world.on&&world.messages.size()==2,"joining a world inside the Bumblezone turns them off");
        System.out.println("PASS: Bumblezone shader switch (entry, staying, TARDIS dimensions ignored both ways, restore on leaving, hand-enabled and already-off shaders left alone, world exit)");
    }
}
