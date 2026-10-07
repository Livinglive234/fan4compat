package dev.fan4.compat.shared;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;

/** Weak object-instance keys: Minecraft entity equality only compares entity IDs. */
public final class WeakIdentityMap<K,V> extends AbstractMap<K,V> {
    private final ReferenceQueue<K> queue=new ReferenceQueue<>();
    private final Map<Key<K>,V> values=new HashMap<>();
    private static final class Key<K> extends WeakReference<K> {
        private final int hash;
        Key(K value,ReferenceQueue<K> queue){super(Objects.requireNonNull(value),queue);hash=System.identityHashCode(value);}
        public int hashCode(){return hash;}
        public boolean equals(Object other){return this==other||(other instanceof Key<?> key&&get()!=null&&get()==key.get());}
    }
    private void drain(){for(var key=queue.poll();key!=null;key=queue.poll())values.remove(key);}
    @SuppressWarnings("unchecked") private Key<K> lookup(Object key){return new Key<>((K)key,null);}
    public V get(Object key){drain();return key==null?null:values.get(lookup(key));}
    public boolean containsKey(Object key){drain();return key!=null&&values.containsKey(lookup(key));}
    public V put(K key,V value){drain();return values.put(new Key<>(key,queue),value);}
    public V remove(Object key){drain();return key==null?null:values.remove(lookup(key));}
    public void clear(){values.clear();drain();}
    public int size(){drain();return values.size();}
    /** Read-only view; each iterator snapshots and holds its live keys. */
    public Set<Entry<K,V>> entrySet(){
        return new AbstractSet<>() {
            public int size(){return WeakIdentityMap.this.size();}
            public Iterator<Entry<K,V>> iterator(){
                drain();List<Entry<K,V>> entries=new ArrayList<>();
                values.forEach((key,value)->{K object=key.get();if(object!=null)entries.add(new SimpleImmutableEntry<>(object,value));});
                return Collections.unmodifiableList(entries).iterator();
            }
        };
    }
}
