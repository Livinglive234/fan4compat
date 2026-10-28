package dev.fan4.compat.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small JSON reader and writer for the standalone bridge tool, which runs from the jar without Gson or Fabric Loader on the class path. Objects are ordered maps,
 * arrays are lists, strings are strings, numbers keep their text ({@link Raw}) so they are written back unchanged, and true, false and null are Boolean and null.
 */
final class MiniJson {
    /** A number as it was written. */
    record Raw(String text) {}

    private final String text;
    private int at;

    private MiniJson(String text){this.text=text;}

    static Object parse(String text){
        MiniJson json=new MiniJson(text);
        json.skip();
        Object value=json.value();
        json.skip();
        if(json.at!=text.length())throw json.error("trailing text");
        return value;
    }

    private Object value(){
        if(at>=text.length())throw error("unexpected end");
        char c=text.charAt(at);
        if(c=='{')return object();
        if(c=='[')return array();
        if(c=='"')return string();
        if(text.startsWith("true",at)){at+=4;return Boolean.TRUE;}
        if(text.startsWith("false",at)){at+=5;return Boolean.FALSE;}
        if(text.startsWith("null",at)){at+=4;return null;}
        int start=at;
        while(at<text.length()&&"+-0123456789.eE".indexOf(text.charAt(at))>=0)at++;
        if(start==at)throw error("unexpected character '"+c+"'");
        return new Raw(text.substring(start,at));
    }
    private Map<String,Object> object(){
        Map<String,Object> out=new LinkedHashMap<>();
        at++;skip();
        if(peek('}')){at++;return out;}
        while(true) {
            skip();
            String key=string();
            skip();expect(':');skip();
            out.put(key,value());
            skip();
            if(peek(',')){at++;continue;}
            expect('}');return out;
        }
    }
    private List<Object> array(){
        List<Object> out=new ArrayList<>();
        at++;skip();
        if(peek(']')){at++;return out;}
        while(true) {
            skip();
            out.add(value());
            skip();
            if(peek(',')){at++;continue;}
            expect(']');return out;
        }
    }
    private String string(){
        expect('"');
        StringBuilder out=new StringBuilder();
        while(at<text.length()) {
            char c=text.charAt(at++);
            if(c=='"')return out.toString();
            if(c!='\\'){out.append(c);continue;}
            if(at>=text.length())break;
            char escape=text.charAt(at++);
            switch(escape) {
                case 'n'->out.append('\n');
                case 't'->out.append('\t');
                case 'r'->out.append('\r');
                case 'b'->out.append('\b');
                case 'f'->out.append('\f');
                case 'u'->{out.append((char)Integer.parseInt(text.substring(at,at+4),16));at+=4;}
                default->out.append(escape);
            }
        }
        throw error("unterminated string");
    }
    private void skip(){while(at<text.length()&&Character.isWhitespace(text.charAt(at)))at++;}
    private boolean peek(char c){return at<text.length()&&text.charAt(at)==c;}
    private void expect(char c){if(!peek(c))throw error("expected '"+c+"'");at++;}
    private IllegalArgumentException error(String what){return new IllegalArgumentException("Invalid JSON at "+at+": "+what);}

    static String write(Object value){
        StringBuilder out=new StringBuilder();
        write(out,value,0);
        return out.append('\n').toString();
    }
    private static void write(StringBuilder out,Object value,int depth){
        if(value==null)out.append("null");
        else if(value instanceof Boolean||value instanceof Raw)out.append(value instanceof Raw raw?raw.text():value.toString());
        else if(value instanceof String s)quote(out,s);
        else if(value instanceof Map<?,?> map) {
            if(map.isEmpty()){out.append("{}");return;}
            out.append("{\n");
            int left=map.size();
            for(Map.Entry<?,?> entry:map.entrySet()) {
                indent(out,depth+1);quote(out,entry.getKey().toString());out.append(": ");write(out,entry.getValue(),depth+1);
                out.append(--left>0?",\n":"\n");
            }
            indent(out,depth);out.append('}');
        } else if(value instanceof List<?> list) {
            if(list.isEmpty()){out.append("[]");return;}
            out.append("[\n");
            for(int i=0;i<list.size();i++) {
                indent(out,depth+1);write(out,list.get(i),depth+1);
                out.append(i<list.size()-1?",\n":"\n");
            }
            indent(out,depth);out.append(']');
        } else throw new IllegalArgumentException("Cannot write "+value.getClass());
    }
    private static void indent(StringBuilder out,int depth){for(int i=0;i<depth;i++)out.append("  ");}
    private static void quote(StringBuilder out,String s){
        out.append('"');
        for(int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            switch(c) {
                case '"'->out.append("\\\"");
                case '\\'->out.append("\\\\");
                case '\n'->out.append("\\n");
                case '\r'->out.append("\\r");
                case '\t'->out.append("\\t");
                default->{if(c<0x20)out.append(String.format("\\u%04x",(int)c));else out.append(c);}
            }
        }
        out.append('"');
    }
}
