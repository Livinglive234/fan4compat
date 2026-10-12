package dev.isxander.yacl3.api;
import java.util.function.*;
import net.minecraft.class_2561;
public class Option<T> extends OptionBase<T> {
    public static <T> Builder<T> createBuilder(){return new Builder<>();}
    public static <T> Builder<T> createBuilder(Class<T> type){return new Builder<>();}
    public static final class Builder<T> {
        private final Option<T> option=new Option<>();private Function<Option<T>,Object> controller;
        public Builder<T> name(class_2561 name){option.name=name;return this;}
        public Builder<T> description(OptionDescription description){option.description=description;return this;}
        public Builder<T> description(Function<T,OptionDescription> description){throw new AssertionError("overload chosen by mistake");}
        public Builder<T> binding(T defaultValue,Supplier<T> getter,Consumer<T> setter){option.defaultValue=defaultValue;option.getter=getter;option.setter=setter;return this;}
        public Builder<T> controller(Function<Option<T>,Object> controller){this.controller=controller;return this;}
        public Builder<T> available(boolean available){option.available=available;return this;}
        public Option<T> build(){option.controller=controller.apply(option);return option;}
    }
}
