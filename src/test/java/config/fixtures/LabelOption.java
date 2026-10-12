package dev.isxander.yacl3.api;
import net.minecraft.class_2561;
public final class LabelOption extends OptionBase<class_2561> {
    public final class_2561 label;
    private LabelOption(class_2561 label){this.label=label;}
    public static LabelOption create(class_2561 label){return new LabelOption(label);}
}
