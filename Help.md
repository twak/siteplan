The best thing to do is watch the [video](http://www.youtube.com/watch?v=BrCDKrBS9To) for a general idea, and refer back to this page for  more details. the 3d window has changed since the video was created.

<a href='http://www.youtube.com/watch?feature=player_embedded&v=BrCDKrBS9To' target='_blank'><img src='http://img.youtube.com/vi/BrCDKrBS9To/0.jpg' width='425' height=344 /></a>

(the meshes are [here](https://github.com/twak/siteplan/raw/master/CampSkeleton/dist/meshes.zip))

# Getting Started #

To run the compiled jar:
 * install [sun's java 1.8+](http://java.sun.com)
 * download [the jar](https://github.com/twak/siteplan/raw/wiki/siteplan-0.0.1-SNAPSHOT.jar)
 * run "java -jar siteplan-0.0.1-SNAPSHOT-jar-with-dependencies.jar"

# To Build #

  * install my deps into maven "mvn compile install" from the same directory as the pom.xml:
    * [jutils](https://github.com/twak/jutils)
    * [campskeleton](https://github.com/twak/campskeleton)
  * clone the siteplan repo
  * compile to a jar with "mvn package" in the same directory as siteplan's pom.xml, this will create a jar in the target directory.
  * There are lots of bugs, report them on github.

# 3D Window Controls #

(designed for nice demo videos, rather than being useful)

* WASD - move forward/strafe
* Mouse - look around

# 2D Mouse Window Controls #

Note: a 3 buttoned mouse is needed for most editing operations just now. Sorry.

*Left - select, drag corners, & anchors. Double click on anchors for additional options.** Control + Left click add stuff - click on a blank space to add a plan/global offset, or a line to add a point.
*Shift + Left click - delete points, delete all the points to remove a plan** Right drag - move view
*Wheel - zoom view** The Alt key will change the behavour of the corner-dragging - in plan mode it snaps to grid (if active), and profile mode it moves all points above the selected point as well.

# 2D Interface Introduction #

The left window is the plan view. The right window is the profile view. Select an edge in the plan by clicking on it, in the left window.
Selecting the "updates" button will update the 3D every time you change something. This can get slow. The go button just creates the 3D for the current design.
After selecting an edge, assign a profile to it from the combo box above the profile window.
Create a new profile using the new profile button.
The plans must not self-intersect ("don't cross the beams"), or be inside out (or you'll get inside out buildings). Courtyards/holes in footprints must be defined in the opposite diretiction. If you add a new plan inside an existing shape, it should reverse the loop for you, but no guarantees.

Edit the profiles in a similar way to the plans, note that they can only move upwards, or horizontally (the "monotone restriction"). To get around this, double-clicking in a profile will add a global event (overhanging roof).

You can [download](https://github.com/twak/siteplan/blob/master/dist/meshes.zip) a zip files of meshes.
