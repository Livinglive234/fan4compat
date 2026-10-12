package dev.isxander.yacl3.api.controller;
public final class TickBoxControllerBuilder {
    public final Object option;
    private TickBoxControllerBuilder(Object option){this.option=option;}
    public static TickBoxControllerBuilder create(dev.isxander.yacl3.api.Option<Boolean> option){return new TickBoxControllerBuilder(option);}
}
