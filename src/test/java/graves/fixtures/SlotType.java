package dev.emi.trinkets.api;
public class SlotType {
    public final TrinketEnums.DropRule rule;
    public SlotType(TrinketEnums.DropRule rule){this.rule=rule;}
    public TrinketEnums.DropRule getDropRule(){return rule;}
}
