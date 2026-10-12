package dev.fan4.compat.shared;

import java.io.IOException;
import java.nio.file.*;

/** Replaces a file with a fully written temporary one, atomically where the file system can, so a crash never leaves half a file. */
public final class AtomicFiles {
    private AtomicFiles() {}
    public static void replace(Path temp,Path target)throws IOException {
        try{Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException unsupported){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
    }
}
