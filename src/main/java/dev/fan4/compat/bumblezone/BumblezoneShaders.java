package dev.fan4.compat.bumblezone;

/**
 * The Bumblezone is best seen without shaders. Entering its dimension turns the shader pack off (once, with a chat message); the
 * shaders come back when the player goes to another dimension. TARDIS dimensions do not count either way: stepping into a TARDIS
 * from the Bumblezone, or out of one into it, changes nothing. Only shaders this class switched off are switched back on, and
 * a player who turns them on again by hand while inside is left alone until the next entry. Pure logic; {@link Host} does the work.
 */
public final class BumblezoneShaders {
    public static final String DIMENSION="the_bumblezone:the_bumblezone";
    public static final String MESSAGE="The Bumblezone is best experienced without shaders";
    /** What the logic needs from the game. */
    public interface Host {
        boolean shadersOn();
        void setShaders(boolean on);
        void message(String text);
    }
    private boolean inside,disabledByUs;

    /** The player's dimension, given again whenever it may have changed (a tick, a join). Cheap when nothing changed. */
    public void dimension(String id,boolean tardis,Host host) {
        if(tardis)return;
        if(DIMENSION.equals(id)) {
            if(inside)return;
            inside=true;
            if(host.shadersOn()){host.setShaders(false);disabledByUs=true;host.message(MESSAGE);}
        } else if(inside) {
            inside=false;restore(host);
        }
    }
    /** The player left the world: give the shaders back, so they are not left off for the next world. */
    public void leftWorld(Host host){inside=false;restore(host);}
    private void restore(Host host) {
        if(!disabledByUs)return;
        disabledByUs=false;
        if(!host.shadersOn())host.setShaders(true);
    }
}
