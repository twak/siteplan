package camp.jme;

import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.util.geom.BufferUtils;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Point3d;
import utils.ObjDump;

/**
 * JME -> obj exporter. ignores texture coords, textures etc...
 *
 * meshes only!
 *
 * @author twak
 */
public class JmeObjDump extends ObjDump
{
    public void add (Node node)
    {
        node.updateGeometricState( 0, true );

        if (node.getChildren() != null)
        for (Spatial s : node.getChildren())
        {
            if (s != null)
                add (s);
        }
    }

    public void add( Spatial s )
    {
        if (s instanceof Node)
            {
                add ((Node)s);
            }
            else if ( s instanceof TriMesh )
            {
                TriMesh tm = (TriMesh) s;

                IntBuffer ib = tm.getIndexBuffer();

                Vector3f[] vertices = BufferUtils.getVector3Array( tm.getVertexBuffer() );

                // transform vertices by spatial's world coordinates
                for ( int i = 0; i < vertices.length; i++ )
                    vertices[i] = s.localToWorld( vertices[i], null );

                for ( int i = 0; i < ib.limit(); i += 3 )
                {
//                    System.out.println (i + " --- "+vertices.length);
                    List<Integer> face = new ArrayList();
                    for ( Point3d p : new Point3d [] {
                        pt (vertices[ib.get( i+0 )]),
                        pt (vertices[ib.get(i+1)]),
                        pt (vertices[ib.get(i+2)]) })
                    {
                        if ( vertexToNo.containsKey( p ) )
                        {
                            face.add( vertexToNo.get( p ) );
                        }
                        else
                        {
                            int number = order.size() + 1; // size will be next index
                            face.add( number );
                            order.add( p );
                            vertexToNo.put( p, number );
                        }
                    }
                    tris.add( face ); // this will just be tris...
                }
            }
    }


    private Point3d pt (Vector3f vec)
    {
        return new Point3d (vec.x, vec.y, vec.z);
    }


}
