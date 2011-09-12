package camp.junk.wiggle;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import straightskeleton.Edge;
import straightskeleton.Machine;
import utils.LoopL;

public class House
{

    Point2d location;
    Vector2d tangent;
    Edge[] base = new Edge[4];
    Machine[] machine = new Machine[]{ new Machine( Math.PI / 8 ), new Machine( Math.PI / 2.0 - 0.01 ), new Machine( Math.PI / 8 ), new Machine( Math.PI / 2.0 - 0.01 ) };
    LoopL<Edge> plot = new LoopL();
    WiggleUI outer;

    int number;

    House( int number )
    {
        this.number = number;
    }

    @Override
    public String toString()
    {
        return "house " + number;
    }
}
