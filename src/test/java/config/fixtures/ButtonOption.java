package dev.isxander.yacl3.api;
import java.util.function.*;
import net.minecraft.class_2561;
public final class ButtonOption extends OptionBase<Object> {
    public class_2561 text;public Consumer<Object> action;
    public static Builder createBuilder(){return new Builder();}
    public static final class Builder {
        private final ButtonOption option=new ButtonOption();
        public Builder name(class_2561 name){option.name=name;return this;}
        public Builder text(class_2561 text){option.text=text;return this;}
        public Builder description(OptionDescription description){option.description=description;return this;}
        public Builder action(BiConsumer<Object,ButtonOption> action){throw new AssertionError("overload chosen by mistake");}
        public Builder action(Consumer<Object> action){option.action=action;return this;}
        public Builder available(boolean available){option.available=available;return this;}
        public ButtonOption build(){return option;}
    }
}
