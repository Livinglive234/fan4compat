import dev.fan4.compat.shared.ShipPose;
import dev.fan4.compat.shared.ShipPose.Point;

public class ShipPoseTest {
    static void close(Point actual,Point expected) {
        if(Math.abs(actual.x()-expected.x())>1e-6||Math.abs(actual.y()-expected.y())>1e-6||Math.abs(actual.z()-expected.z())>1e-6) throw new AssertionError(actual+" != "+expected);
    }
    public static void main(String[] args) {
        // Shipyard pivot is millions of blocks away. Rotate 90 degrees and place at (10,70,20).
        double[] matrix={0,0,-1,0, 0,1,0,0, 1,0,0,0, -9999989, -60, -2866565, 1};
        ShipPose ship=new ShipPose(matrix);Point block=new Point(-2866585,130,9999999);
        close(ship.position(block),new Point(10,70,20));
        Point face=new Point(0,0,1);
        close(ship.position(ShipPose.door(block,face)),new Point(11,71,19.5));
        close(ship.direction(face),new Point(1,0,0));
        if(!ShipPose.cardinal(ship.direction(face)).equals("east")) throw new AssertionError("Facing must rotate with ship");
        // Same local destination after the ship translates, rather than yesterday's world position.
        matrix[12]+=100;matrix[13]+=4;matrix[14]-=80;
        close(new ShipPose(matrix).position(block),new Point(110,74,-60));
        close(ship.position(block),new Point(10,70,20)); // immutable snapshot
        // Roll transforms portal up, without adding translation to the basis vectors.
        ShipPose rolled=new ShipPose(new double[]{1,0,0,0,0,0,1,0,0,-1,0,0,4,5,6,1});
        close(rolled.direction(new Point(0,1,0)),new Point(0,0,1));
        close(rolled.position(new Point(1,2,3)),new Point(5,2,8));
        System.out.println("PASS: shipyard coordinates, door facing, translation, rotation, roll and fixed local summon target");
    }
}
