package camp.jme;

import com.jme.input.InputHandler;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.action.InputActionEvent;
import com.jme.input.action.KeyInputAction;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import utils.MUtils;

public class SpinCameraController extends InputHandler
{

    protected Vector3f worldUpVec = new Vector3f( Vector3f.UNIT_Y.clone() );
    private float angle = 100f;
    private float elevation = 0.2f;
    private float distance = 25.0F;
    // interpolated, smoothed values
    private float angleS = 0;
    private float elevationS = 1;
    private float distanceS = 25.0F;
    // the middle of the thing we're looking at
    protected Vector3f targetOffset = new Vector3f(0,5,0);
    protected Vector3f targetOffsetS = new Vector3f(0,5,0);
    boolean rot = true;
    Camera cam;
    private boolean lock = false;

    public SpinCameraController( Camera cam ) {
        super();
        this.cam = cam;
        KeyBindingManager keyboard = KeyBindingManager.getKeyBindingManager();
        keyboard.set( "forward", KeyInput.KEY_Q );
        keyboard.set( "backward", KeyInput.KEY_Z );
        keyboard.set( "lookUp", KeyInput.KEY_W );
        keyboard.set( "lookDown", KeyInput.KEY_S );
        keyboard.set( "strafeLeft", KeyInput.KEY_A );
        keyboard.set( "strafeRight", KeyInput.KEY_D );
        keyboard.set( "rotate", KeyInput.KEY_SPACE );
        keyboard.set( "hold", KeyInput.KEY_E );
        addAction( sLeft, "strafeLeft", true );
        addAction( sRight, "strafeRight", true );
        addAction( sUp, "lookUp", true );
        addAction( sDown, "lookDown", true );
        addAction( sIn, "forward", true );
        addAction( sOut, "backward", true );
        addAction( sRot, "rotate", false );
        addAction( sLock, "hold", false );
    }
    KeyInputAction sLeft = new KeyInputAction()
    {

        public void performAction( InputActionEvent evt ) {
            angle += evt.getTime();
        }
    };
    KeyInputAction sRight = new KeyInputAction()
    {

        public void performAction( InputActionEvent evt ) {
            angle -= evt.getTime();
        }
    };
    KeyInputAction sUp = new KeyInputAction()
    {

        public void performAction( InputActionEvent evt ) {
//            elevation = (float) (MUtils.clamp( elevation + evt.getTime(), -Math.PI * 4.0 / 10, Math.PI * 4.0 / 10 ));
            elevation = (float) (MUtils.clamp( elevation + evt.getTime(), -Math.PI, Math.PI/2 ));
        }
    };
    KeyInputAction sDown = new KeyInputAction()
    {

        public void performAction( InputActionEvent evt ) {
//            elevation = (float) (MUtils.clamp( elevation - evt.getTime(), -Math.PI * 4.0 / 10, Math.PI * 4.0 / 10  ));
            elevation = (float) (MUtils.clamp( elevation - evt.getTime(), -Math.PI, Math.PI/2 ));
        }
    };
    KeyInputAction sIn = new KeyInputAction()
    {

        public void performAction( InputActionEvent evt ) {
            distance = (float) (MUtils.clamp( distance / (1 + evt.getTime()), 1, 500 ));
        }
    };
    KeyInputAction sOut = new KeyInputAction()
    {

        public void performAction( InputActionEvent evt ) {
            distance = (float) (MUtils.clamp( distance * (1 + evt.getTime()), 1, 500 ));
        }
    };
    KeyInputAction sRot = new KeyInputAction()
    {

        public void performAction( InputActionEvent evt ) {
            rot = !rot;
        }
    };
    KeyInputAction sLock = new KeyInputAction()
    {
        public void performAction( InputActionEvent evt ) {
            lock = !lock;
        }
    };

    public void update( float time ) {
        if ( !isEnabled() )
            return;

        super.update( time );
        
        if ( !lock )
        {
            if ( rot )
            angle += time/4.;
        float frac = 0.01F;
        angleS = angleS - (angleS - angle) * frac;
        elevationS = elevationS - (elevationS - elevation) * frac;
        distanceS = distanceS - (distanceS - distance) * frac;

        targetOffsetS = new Vector3f (
                targetOffsetS.x - (targetOffsetS.x - targetOffset.x) * frac,
                targetOffsetS.y - (targetOffsetS.y - targetOffset.y) * frac,
                targetOffsetS.z - (targetOffsetS.z - targetOffset.z) * frac );

//            System.out.println("a: "+angle);
//            System.out.println("e: "+elevation);
//            System.out.println("d: "+distance);
        Vector3f pos = cam.getLocation();
        Matrix3f ele = new Matrix3f();
        ele.fromAngleAxis( elevationS, new Vector3f( 0, 0, 1 ) );
        Matrix3f rot = new Matrix3f();
        rot.fromAngleAxis( angleS, new Vector3f( 0, 1, 0 ) );
        rot = rot.mult( ele );
        pos.set( distanceS, 0, 0 );
        pos.set( rot.mult( pos ) );

        pos.addLocal( targetOffsetS );

        // Look at our target
        cam.lookAt( targetOffsetS, worldUpVec );
        }
        cam.update();
    }
}
