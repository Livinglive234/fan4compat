package dev.emi.trinkets.api;
public class SlotType {
    public final String id;public final TrinketEnums.DropRule rule;
    public SlotType(String id,TrinketEnums.DropRule rule){this.id=id;this.rule=rule;}
    public TrinketEnums.DropRule getDropRule(){return rule;}
    public String getId(){return id;}
    public java.util.Set<Object> getValidatorPredicates(){return java.util.Set.of();}
}
