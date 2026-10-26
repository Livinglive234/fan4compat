package net.fabricmc.fabric.api.client.screen.v1;

import net.fabricmc.fabric.api.event.Event;

public final class ScreenEvents {
    public static final Event AFTER_INIT=new Event();
    public interface AfterInit {void afterInit(Object client,Object screen,int scaledWidth,int scaledHeight);}
}
