package tegula.tiling;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

/**
 * Hyperbolic geometry
 * Uses the Minkowski (=hyperboloid) model. The hyperboloid is defined by z^2 = x^2 + y^2 + 1 (here scaled by factor 100).
 * The isometry group preserving orientation and leaving hyperboloid invariant is generated by rotation matrices around
 * z-axis and matrices of the form
 * <p>
 * 1  0        0
 * L_t =  0  cosh(t)  sinh(t)
 * 0  sinh(t)  cosh(t)
 * <p>
 * Created by Zeller on 4/27/16.
 */

public class HyperbolicGeometry {
    private static final Point3D Y_AXIS = new Point3D(0, 1, 0);
    private static final Point3D Z_AXIS = new Point3D(0, 0, 1);
    private static final Affine reflection = new Affine(-1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);  // Reflection at y-z plane


    /**
     * creates a transformation from a1-b1 to a2-b2, assuming that the distance between both pairs of points is the same.
     *
     * @param a1              source point
     * @param b1              source point
     * @param a2              target point
     * @param b2              target point
     * @param keepOrientation keep orientation on hyperboloid
     * @return transformation
     */

    public static Transform createTransform(Point3D a1, Point3D b1, Point3D a2, Point3D b2, boolean keepOrientation) {
        final Transform a1ToZero = matchZero(a1, false).createConcatenation(matchVec(a1, Y_AXIS, false)); // Maps a1 to minimal point of hyperboloid
        final Transform a2ToZero = matchZero(a2, false).createConcatenation(matchVec(a2, Y_AXIS, false)); // Maps a2 to minimal point of hyperboloid
        final Point3D b2Zero = a2ToZero.transform(b2);

        if (keepOrientation) {
            final Point3D b1Zero = a1ToZero.transform(b1);
            return matchVec(a2, Y_AXIS, true)   // Rotates geodesic in y-z-plane to geodesic through a2 and minimum of hyperboloid
                    .createConcatenation(matchZero(a2, true))   // Raises minimum of hyperboloid to z-level of a2 along geodesic in y-z-plane
                    .createConcatenation(matchVec(b1Zero, b2Zero, false))   // Rotates b1Zero to b2Zero
                    .createConcatenation(a1ToZero);     // Maps a1 to minimum of hyperboloid

        } else {
            final Point3D b1Zero = reflection.createConcatenation(a1ToZero).transform(b1);

            return matchVec(a2, Y_AXIS, true)
                    .createConcatenation(matchZero(a2, true))
                    .createConcatenation(matchVec(b1Zero, b2Zero, false))
                    .createConcatenation(reflection)    // Adds reflection to process defined for case keepOrientation = true
                    .createConcatenation(a1ToZero);
        }
    }

    public static Transform matchVec(Point3D a, Point3D vec, boolean Inv) {     // Rotation mapping geodesic through "a" and minimum to geodesic through "vec" and minimum by a rotation around Z_Axis
        final Point2D a2D = new Point2D(a.getX(), a.getY());    // Consider x and y components to calculate angle of rotation
        final Point2D vec2D = new Point2D(vec.getX(), vec.getY());

        final Transform rot1;   // Rotation
        final Point3D rot1Axis; // Rotation axis
        final double rot1Angle = a2D.angle(vec2D); // Rotation angle

        if (a2D.getX() * vec2D.getY() - vec2D.getX() * a2D.getY() >= 0) {
            rot1Axis = Z_AXIS;    // Counter-clockwise rotation
        } else {
            rot1Axis = Z_AXIS.multiply(-1);   // Clockwise rotation
        }

        if (Inv) {   // Calculates the inverse rotation if Inv is set to true
            rot1 = new Rotate(-rot1Angle, rot1Axis);
        } else {
            rot1 = new Rotate(rot1Angle, rot1Axis);
        }
        return rot1;
    }


    public static Affine matchZero(Point3D a, boolean Inv) {     // Maps a given point "a" which is in the intersection of y-z-plane and hyperboloid to the minimum of the hyperboloid
        double aRot1Y = Math.sqrt(a.getX() * a.getX() + a.getY() * a.getY());   // distance of "a" from z-axis


        final double factor = 100;
        final Affine trans;
        if (Inv) {   // Calculates inverse if Inv = true
            trans = new Affine(1, 0, 0, 0, 0, a.getZ() / factor, aRot1Y / factor, 0, 0, aRot1Y / factor, a.getZ() / factor, 0);     // Inverse of isometry described below
        } else {
            trans = new Affine(1, 0, 0, 0, 0, a.getZ() / factor, -aRot1Y / factor, 0, 0, -aRot1Y / factor, a.getZ() / factor, 0);   // Isometry leaving y-z-plane and hyperboloid invariant. Maps a point on geodesic in y-z-plane to minimum of hyperboloid
        }
        return trans;
    }


}