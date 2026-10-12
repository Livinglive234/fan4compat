package dev.isxander.yacl3.api;
import java.util.*;
import net.minecraft.class_2561;
public final class OptionDescription {
    public final List<String> paragraphs=new ArrayList<>();
    public static OptionDescription of(class_2561... text){OptionDescription d=new OptionDescription();for(class_2561 t:text)d.paragraphs.add(t.text);return d;}
}
