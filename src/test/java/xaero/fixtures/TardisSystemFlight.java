package net.drgmes.dwm.common.tardis.systems;
public final class TardisSystemFlight {
    private String step="NONE";public boolean flying;public int percent;
    public boolean inProgress(){return !step.equals("NONE");}public boolean isInFlight(){return flying;}public int getProgressPercent(){return percent;}
    public void set(String step,boolean flying,int percent){this.step=step;this.flying=flying;this.percent=percent;}
}
