package dev.isxander.yacl3.api;
import java.util.function.*;
import net.minecraft.class_2561;
/** Shared state of the fixture options; the real API uses interfaces, whose statics are not inherited. */
public class OptionBase<T> {
    public class_2561 name;public OptionDescription description;public T defaultValue;public Supplier<T> getter;public Consumer<T> setter;
    public Object controller;public boolean available=true;
}
