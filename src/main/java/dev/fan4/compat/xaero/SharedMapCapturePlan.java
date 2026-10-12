package dev.fan4.compat.xaero;

import java.util.*;

/**
 * Which chunks near players the server should summarize next. "Explored" means a real player's client has the chunk: the square
 * of loaded chunks around each player. Chunks are taken nearest-first, never-seen before stale, within a per-call budget, and
 * looked at again only after a delay so edits are picked up without re-reading the same chunk every tick.
 */
public final class SharedMapCapturePlan {
    public record Player(String dimension,int chunkX,int chunkZ,int radius) {}
    public record Target(String dimension,int x,int z) {}
    private static final int MAX_TRACKED=200_000;
    private final long recheckTicks;
    private final Map<String,Map<Long,Long>> seen=new HashMap<>();
    public SharedMapCapturePlan(long recheckTicks){this.recheckTicks=recheckTicks;}

    /** Positions around a player, nearest ring first. */
    private static final class Spiral {
        final Player player;int ring,step;
        Spiral(Player player){this.player=player;}
        /** The next position, or null when the whole square has been walked. */
        int[] next() {
            while(ring<=player.radius()) {
                int side=ring*2+1,perimeter=ring==0?1:4*(side-1);
                if(step>=perimeter){ring++;step=0;continue;}
                int dx,dz,i=step++;
                if(ring==0){dx=0;dz=0;}
                else if(i<side-1){dx=-ring+i;dz=-ring;}
                else if(i<2*(side-1)){dx=ring;dz=-ring+(i-(side-1));}
                else if(i<3*(side-1)){dx=ring-(i-2*(side-1));dz=ring;}
                else{dx=-ring;dz=ring-(i-3*(side-1));}
                return new int[]{player.chunkX()+dx,player.chunkZ()+dz};
            }
            return null;
        }
    }
    /** Up to {@code budget} chunks due for capture at {@code now} (server ticks), one player at a time in turn; each is marked as looked at. */
    public List<Target> next(List<Player> players,long now,int budget) {
        List<Target> due=new ArrayList<>();
        List<Spiral> active=new ArrayList<>();for(Player player:players)active.add(new Spiral(player));
        while(due.size()<budget&&!active.isEmpty()) {
            for(Iterator<Spiral> it=active.iterator();it.hasNext()&&due.size()<budget;) {
                Spiral spiral=it.next();Map<Long,Long> dimension=seen.computeIfAbsent(spiral.player.dimension(),unused->new HashMap<>());
                int[] position;boolean found=false;
                while(!found&&(position=spiral.next())!=null) {
                    long key=SharedMapChunk.key(position[0],position[1]);Long last=dimension.get(key);
                    if(last!=null&&now-last<recheckTicks)continue;
                    dimension.put(key,now);due.add(new Target(spiral.player.dimension(),position[0],position[1]));found=true;
                }
                if(!found)it.remove();
            }
        }
        prune(players);
        return due;
    }
    /** Forget chunks far from every player so the table stays bounded. */
    private void prune(List<Player> players) {
        int tracked=0;for(var dimension:seen.values())tracked+=dimension.size();
        if(tracked<=MAX_TRACKED)return;
        for(var entry:seen.entrySet()) {
            List<Player> here=players.stream().filter(p->p.dimension().equals(entry.getKey())).toList();
            entry.getValue().keySet().removeIf(key->{
                int x=(int)(key>>32),z=(int)key.longValue();
                for(Player p:here)if(Math.abs(x-p.chunkX())<=p.radius()+4&&Math.abs(z-p.chunkZ())<=p.radius()+4)return false;
                return true;
            });
        }
    }
}
