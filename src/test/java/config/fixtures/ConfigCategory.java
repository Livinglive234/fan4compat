package dev.isxander.yacl3.api;
import java.util.*;
import net.minecraft.class_2561;
public final class ConfigCategory {
    public class_2561 name;public final List<Object> options=new ArrayList<>();
    public static Builder createBuilder(){return new Builder();}
    public static final class Builder {
        private final ConfigCategory category=new ConfigCategory();
        public Builder name(class_2561 name){category.name=name;return this;}
        public Builder option(OptionBase<?> option){category.options.add(option);return this;}
        public Builder option(java.util.function.Supplier<OptionBase<?>> option){return option(option.get());}
        public Builder options(Collection<? extends OptionBase<?>> options){category.options.addAll(options);return this;}
        public ConfigCategory build(){return category;}
    }
}
