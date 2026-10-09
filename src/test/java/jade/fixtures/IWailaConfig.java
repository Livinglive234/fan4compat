package snownee.jade.api.config;
public class IWailaConfig {
    public static final IWailaConfig CONFIG=new IWailaConfig();
    public Perspective mode=Perspective.EYE;
    public enum Perspective {EYE,CAMERA}
    public static IWailaConfig get(){return CONFIG;}
    public IWailaConfig getGeneral(){return this;}
    public Perspective getPerspectiveMode(){return mode;}
}
