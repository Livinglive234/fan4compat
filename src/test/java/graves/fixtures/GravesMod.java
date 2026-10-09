package com.kador.graves;
public final class GravesMod {
    public static class Retrieval {public boolean dropOverflowOnQuickRetrieve;}
    public static class Config {public Retrieval retrieval=new Retrieval();}
    public static final Config CONFIG=new Config();
    public static Config getConfig(){return CONFIG;}
}
