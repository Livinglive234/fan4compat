package dev.fan4.compat.shared;

/** Turns a hook off for the session after its first failure and logs one warning; the hook keeps its own latch and checks {@link #tripped()} before each call. */
public final class FailureLatch {
    private final String feature;
    private volatile boolean tripped;

    public FailureLatch(String feature){this.feature=feature;}

    public boolean tripped(){return tripped;}
    public void trip(Throwable failure){
        if(!tripped) {
            tripped=true;
            try{GameLog.warn(feature+" disabled after a failure: "+failure.getClass().getSimpleName()+": "+failure.getMessage());}catch(RuntimeException|LinkageError ignored){}
        }
    }
    /** For tests: clears the latch. */
    public void reset(){tripped=false;}
}
