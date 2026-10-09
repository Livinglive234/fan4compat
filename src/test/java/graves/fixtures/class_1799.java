package net.minecraft;
public class class_1799 {
    public static final class_1799 field_8037=new class_1799(0);
    public int count;
    public String item="accessory";
    public boolean vanishing;
    public Object method_7909(){return item;}
    public class_1799(int count){this.count=count;}
    public boolean method_7960(){return count==0;}
    public class_1799 method_7972(){class_1799 copy=new class_1799(count);copy.item=item;copy.vanishing=vanishing;return copy;}
    public static boolean method_31577(class_1799 left,class_1799 right){return left.item.equals(right.item);}
    public int method_7947(){return count;}
}
