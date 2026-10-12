package dev.fan4.compat.xaero;

import java.util.Locale;

/**
 * Where the shared map's terrain comes from, set by the server operator ({@code sharedMapMode} in
 * {@code config/fan4compat-xaero.properties}). NORMAL leaves Xaero alone: no capture, no uploads, nothing sent.
 */
public enum SharedMapMode {
    /** The server summarizes the chunks it has loaded around players. */
    SERVER(true,false),
    /** Clients upload what Xaero has mapped; the server does not capture. */
    CLIENT(false,true),
    /** Server capture, plus client uploads for places the server has not captured. */
    BOTH(true,true),
    /** Xaero's default behavior, no server involvement. */
    NORMAL(false,false);
    public final boolean captures,acceptsUploads;
    SharedMapMode(boolean captures,boolean acceptsUploads){this.captures=captures;this.acceptsUploads=acceptsUploads;}
    /** Anything unreadable falls back to the default, BOTH. */
    public static SharedMapMode parse(String text) {
        if(text!=null)try{return valueOf(text.trim().toUpperCase(Locale.ROOT));}catch(IllegalArgumentException unknown){}
        return BOTH;
    }
    /** Server and client share terrain, in either direction. */
    public boolean active(){return this!=NORMAL;}
    public static SharedMapMode fromOrdinal(int ordinal){return ordinal>=0&&ordinal<values().length?values()[ordinal]:NORMAL;}
}
