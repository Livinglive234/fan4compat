package dev.isxander.yacl3.api;
import java.util.*;
import net.minecraft.class_2561;
/** Records what the settings screen asks of YACL; method names, arities and overloads mirror 3.8.2. */
public final class YetAnotherConfigLib {
    public class_2561 title;public final List<ConfigCategory> categories=new ArrayList<>();public Runnable save;public Object parent;
    public static Builder createBuilder(){return new Builder();}
    public Object generateScreen(Object parent){this.parent=parent;return this;}
    public static final class Builder {
        private final YetAnotherConfigLib lib=new YetAnotherConfigLib();
        public Builder title(class_2561 title){lib.title=title;return this;}
        public Builder category(ConfigCategory category){lib.categories.add(category);return this;}
        public Builder category(java.util.function.Supplier<ConfigCategory> category){return category(category.get());}
        public Builder save(Runnable save){lib.save=save;return this;}
        public YetAnotherConfigLib build(){return lib;}
    }
}
