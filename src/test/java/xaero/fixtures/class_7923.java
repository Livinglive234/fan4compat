package net.minecraft;
public final class class_7923 {
    public static final Registry field_41175=new Registry();
    public static final class Registry {public Object method_10223(Object id){return new Block(String.valueOf(id));}
        public Object method_10221(Object block){return class_2960.method_60654(((Block)block).id);}}
    public static final class Block {public final String id;public Block(String id){this.id=id;}public Object method_9564(){return new State(this);}}
    public static final class State {final Block block;State(Block block){this.block=block;}public Object method_26204(){return block;}public String toString(){return "state:"+block.id;}}
}
