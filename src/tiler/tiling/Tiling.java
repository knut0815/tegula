package tiler.tiling;

import com.sun.javafx.geom.transform.Affine3D;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.*;
import javafx.util.Pair;
import tiler.core.dsymbols.DSymbol;
import tiler.core.dsymbols.FDomain;
import tiler.core.dsymbols.OrbifoldGroupName;
import tiler.core.fundamental.data.ECR;
import tiler.core.fundamental.data.NCR;
import tiler.core.fundamental.data.OCR;
import tiler.main.Document;
import tiler.util.JavaFXUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * a tiling
 * Created by huson on 4/11/16.
 */
public class Tiling {
    boolean verbose = false;

    private final DSymbol ds;
    private final String groupName;
    private final FDomain fDomain;

    private final Transforms generators;
    private final Constraints constraints;

    private boolean resetHyperbolic = false;
    private boolean resetEuclidean = false;
    public Transform transformFDEuclidean, transformFDHyperbolic;

    public static Point3D refPointHyperbolic = new Point3D(0, 0, 1);
    public static Point3D refPointEuclidean = new Point3D(1,1,0);

    public static Group recycler = new Group();
    public static Transform transformRecycler = new Translate();


    private final int[] flag2vert;
    private final int[] flag2edge;
    private final int[] flag2tile;

    private final int[] vert2flag;
    private final int[] edge2flag;
    private final int[] tile2flag;

    private final int numbVert;
    private final int numbEdge;
    private final int numbTile;


    /**
     * constructor
     *
     * @param ds
     */
    public Tiling(DSymbol ds) {
        this.ds = ds;
        this.groupName = OrbifoldGroupName.getGroupName(ds);
        this.fDomain = new FDomain(ds);
        this.constraints = new Constraints(ds.size());
        this.generators = new Transforms(ds.size());

        flag2vert = new int[ds.size() + 1];
        numbVert = ds.countOrbits(1, 2);
        vert2flag = new int[numbVert + 1];

        for (int a = 1, count = 1; a <= ds.size(); a = ds.nextOrbit(1, 2, a, flag2vert, count++)) // this also sets flag2vert
            vert2flag[count] = a;
        flag2edge = new int[ds.size() + 1];
        numbEdge = ds.countOrbits(0, 2);
        edge2flag = new int[numbEdge + 1];

        for (int a = 1, count = 1; a <= ds.size(); a = ds.nextOrbit(0, 2, a, flag2edge, count++)) // this also sets flag2vert
            edge2flag[count] = a;
        flag2tile = new int[ds.size() + 1];
        numbTile = ds.countOrbits(0, 1);
        tile2flag = new int[numbTile + 1];

        for (int a = 1, count = 1; a <= ds.size(); a = ds.nextOrbit(0, 1, a, flag2tile, count++)) // this also sets flag2vert
            tile2flag[count] = a;
    }

    /**
     * set up the constraints and symmetry group generators
     */
    private void computeConstraintsAndGenerators() {
        generators.getTransforms().clear();

        boolean found = false;
        int a0;
        int i0 = 0;
        for (a0 = 1; a0 <= ds.size(); a0++) {
            for (i0 = 0; i0 <= 2; i0++) {
                if (fDomain.isBoundaryEdge(i0, a0)) {
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }
        if (!found)
            throw new RuntimeException("computeConstraintsAndMore(): Can't find boundary edge");

        if (verbose)
            System.err.println(String.format("Init. boundary edge: a0 %d i0 %d\n", a0, i0));

        int aa = a0;
        int ii = i0;

        int j0 = computeLowIndex(i0);
        int k0 = computeHighIndex(i0);
        int i = i0;
        int j = k0;
        int k = j0;
        int a = a0;

        found = false;
        int count = 0;

        do {
            a0 = a;
            i0 = i;
            j0 = k;
            k0 = j;
            int[] array = nextBoundaryEdge(i0, j0, k0, a0);
            i = array[0];
            j = array[1];
            k = array[2];
            a = array[3];

            if (ds.getVij(i0, j0, a0) > 1 || isSplitOrbit(i0, j0, a0))
                found = true;
            if (++count > 1000)
                break;
        }
        while (!found);

        if (!found && verbose)
            System.err.println("Can't find boundary corner");


        boolean circle_boundary = false;
        if (!found) {
            a0 = aa;
            i0 = ii;

            j0 = computeLowIndex(i0);
            k0 = computeHighIndex(i0);
            circle_boundary = true;
        }

        if (verbose)
            System.err.println(String.format("Boundary edge before corner: a0 %d i0 %d j0 %d k0 %d\n", a0, i0, j0, k0));

        int a00 = a0;
        int i00 = i0;
        int j00 = j0;

        count = 0;
        do {
            int[] array = nextBoundaryEdge(i0, j0, k0, a0);
            i = array[0];
            j = array[1];
            k = array[2];
            a = array[3];

            int b = ds.getSi(i, a);
            if (circle_boundary || (ds.getVij(i, j, a) > 1) || isSplitOrbit(i, j, a)) {
                if (verbose)
                    System.err.println(String.format("Starting new section: vij: %d split_orbit: %d\n",
                            ds.getVij(i, j, a), isSplitOrbit(i, j, a) ? 1 : 0));
                count++;
                setConstraint(0, k, a, constraints, Constraints.ConstraintType.FIXED);
                {
                    Point3D pt0 = fDomain.getVertex3D(k, a);
                    Point3D pt1 = fDomain.getVertex3D(j, a);
                    Point3D pt2 = fDomain.getVertex3D(k, b);
                    Point3D pt3 = fDomain.getVertex3D(j, b);

                    boolean keepOrientation = (fDomain.getOrientation(a) != fDomain.getOrientation(b));

                    Transform transform = getTransform(fDomain.getGeometry(), pt0, pt1, pt2, pt3, keepOrientation);
                    generators.set(i, a, transform);
                }

                if (a == b) {
                    Point2D aPt = fDomain.getVertex(k, a);
                    Point2D bPt = fDomain.getVertex(j, a);
                    constraints.setLineConstraint(1, i, a, new Pair<>(aPt, bPt));
                } else
                    constraints.setConstraint(1, i, a, Constraints.ConstraintType.SYMMETRIC_BOUNDARY);

                if (verbose)
                    System.err.println(String.format("New section: a %d i %d\n", a, i));

            } else {
                generators.setAgain(i, a); // same transform as previously set
                if (a == b) {
                    setConstraint(0, k, a, constraints, Constraints.ConstraintType.LINE); // this will use previously set line
                    constraints.setConstraint(1, i, a, Constraints.ConstraintType.LINE);
                } else {
                    setConstraint(0, k, a, constraints, Constraints.ConstraintType.SYMMETRIC_BOUNDARY);
                    constraints.setConstraint(1, i, a, Constraints.ConstraintType.SYMMETRIC_BOUNDARY);
                }
            }
            a0 = a;
            i0 = i;
            j0 = k;
            k0 = j;
            if (circle_boundary)
                break;
        }
        while (!(a0 == a00 && i0 == i00 && j0 == j00));

        if (count == 0)
            throw new RuntimeException("constraints_and_more(): Can't find boundary section");

        if (verbose)
            System.err.println(String.format("Found %d boundary sections\n", count));
    }

    /**
     * set a constraint on all vertices in the appropriate i,j-orbit
     *
     * @param kind
     * @param k
     * @param a
     * @param constraints
     * @param type
     */
    private void setConstraint(int kind, int k, int a, Constraints constraints, Constraints.ConstraintType type) {
        final int i = computeLowIndex(k);
        final int j = computeHighIndex(k);

        int b = a;

        do {
            constraints.setConstraint(kind, k, b, type);
            b = ds.getSi(i, b);
            constraints.setConstraint(kind, k, b, type);
            b = ds.getSi(j, b);
        }
        while (b != a);
    }

    /**
     * is this orbit split into multiple pieces?
     *
     * @param i
     * @param j
     * @param a
     * @return true, if split
     */
    private boolean isSplitOrbit(int i, int j, int a) {
        int b = a;
        int count = 0;

        do {
            if (fDomain.isBoundaryEdge(i, b))
                count++;
            b = ds.getSi(i, b);
            if (fDomain.isBoundaryEdge(j, b))
                count++;
            b = ds.getSi(j, b);
        }
        while (b != a);
        return count > 2;
    }

    /**
     * computes the smallest integer between 0 and 2 that does not equal i
     *
     * @param i
     * @return smallest index
     */
    public static int computeLowIndex(final int i) {
        return i > 0 ? 0 : 1;
    }

    /**
     * computes the largest integer between 0 and 2 that does not equal i
     *
     * @param i
     * @return smallest index
     */
    public static int computeHighIndex(final int i) {
        return i < 2 ? 2 : 1;
    }

    /**
     * finds the next boundary edge
     *
     * @param i0
     * @param j0
     * @param k0
     * @param a0
     * @return i, j, k and a for next boundary edge
     */
    private int[] nextBoundaryEdge(final int i0, final int j0, final int k0, final int a0) {
        int a00 = a0;

        if (!fDomain.isBoundaryEdge(i0, a00))
            throw new RuntimeException(String.format("nextBoundaryEdge(i0=%d,j0=%d,k0=%d,a0=%d): (a0=%d,i0=%d) not on boundary",
                    i0, j0, k0, a00, a00, i0));

        if (fDomain.isBoundaryEdge(j0, a00)) {
            return new int[]{j0, i0, k0, a00};
        }

        do {
            a00 = ds.getSi(j0, a00);
            if (fDomain.isBoundaryEdge(i0, a00)) {
                return new int[]{i0, j0, k0, a00};
            }
            a00 = ds.getSi(i0, a00);
            if (fDomain.isBoundaryEdge(j0, a00)) {
                return new int[]{j0, i0, k0, a00};
            }
        }
        while (a00 != a0);

        throw new RuntimeException(String.format("nextBoundaryEdge(i0=%d,j0=%d,k0=%d,a0=%d): %s",
                i0, j0, k0, a00, "Can't find other end of boundary orbit"));
    }

    /**
     * get the transform that maps a1-b1 to a2-b2, keeping orientation, if desired
     *
     * @param geom
     * @param a1
     * @param b1
     * @param a2
     * @param b2
     * @param keepOrientation
     * @return transform
     */
    public static Transform getTransform(FDomain.Geometry geom, Point3D a1, Point3D b1, Point3D a2, Point3D b2, boolean keepOrientation) {
        switch (geom) {
            default:
            case Euclidean:
                return EuclideanGeometry.createTransform(a1, b1, a2, b2, keepOrientation);
            case Spherical:
                return SphericalGeometry.createTransform(a1, b1, a2, b2, keepOrientation);
            case Hyperbolic:
                return HyperbolicGeometry.createTransform(a1, b1, a2, b2, keepOrientation);
        }
    }

    public String getGroupName() {
        return groupName;
    }

    /**
     * gets the status line
     *
     * @return status line
     */
    public String getStatusLine() {
        return String.format("Tiling: %d.%d  Vertices: %d  Edges: %d  Tiles: %d  Symmetry group: %s",
                ds.getNr1(), ds.getNr2(), numbVert, numbEdge, numbTile, getGroupName());
    }

//----------------------------------------------------------------------------------------------------------------------
    /**
     * create the set of tiles to be shown in spherical case
     *
     * @return tiles
     */
    public Group createTilingSpherical(boolean drawFundamentalDomainOnly) {
        final Group group = new Group();
        final Group fund = FundamentalDomain.buildFundamentalDomain(ds, fDomain);
        group.getChildren().addAll(fund);

        computeConstraintsAndGenerators();


        // Make copies of fundamental domain.
        if (!drawFundamentalDomainOnly) {
            final OctTree seen = new OctTree();
            final Point3D refPoint = fDomain.getChamberCenter3D(1).multiply(0.01);
            seen.insert(fDomain, refPoint); //root node of OctTree is point of reference.

            final Queue<Transform> queue = new LinkedList<>();
            queue.addAll(generators.getTransforms());

            for (Transform g : generators.getTransforms()) {  // Makes copies of fundamental domain by using generators
                Point3D genRef = g.transform(refPoint);
                if (seen.insert(fDomain, genRef)) {    // Checks whether point "genRef" is in OctTree "seen". Adds it if not.
                        Group group2 = JavaFXUtils.copyFundamentalDomain(fund);
                    group2.getTransforms().add(g);
                        group.getChildren().add(group2);
                    }
            }

            while (queue.size() > 0 && queue.size() < 800) {
                final Transform t = queue.poll(); // remove t from queue

                for (Transform g : generators.getTransforms()) {
                    Transform tg = t.createConcatenation(g);
                    Point3D bpt = tg.transform(refPoint);
                    if (seen.insert(fDomain, bpt)) {
                        Group group2 = JavaFXUtils.copyFundamentalDomain(fund);
                        group2.getTransforms().add(tg);
                        group.getChildren().add(group2);
                        queue.add(tg);
                    }

                    Transform gt = g.createConcatenation(t);
                    bpt = gt.transform(refPoint);
                    if (seen.insert(fDomain, bpt)) {
                        Group group2 = JavaFXUtils.copyFundamentalDomain(fund);
                        group2.getTransforms().add(gt);
                        group.getChildren().add(group2);
                        queue.add(gt);
                    }
                    }
                }
        }
        return group;
    }

//----------------------------------------------------------------------------------------------------------------------
    /**
     * create tiling in hyperbolic case
     *
     * @param maxDist
     * @return group
     */
    public Group createTilingHyperbolic(boolean drawFundamentalDomainOnly, double maxDist) {

        refPointHyperbolic = fDomain.getChamberCenter3D(1).multiply(0.01);
        final OctTree seen = new OctTree();
        seen.insert(fDomain, refPointHyperbolic); // root of OctTree is point of reference

        final Group group = new Group();
        final Group fund = FundamentalDomain.buildFundamentalDomain(ds, fDomain);
        group.getChildren().addAll(fund);

        if (!drawFundamentalDomainOnly) {
            //Add all generators
            computeConstraintsAndGenerators();

            // Make copies of fundamental domain.
            final Queue<Transform> queue = new LinkedList<>();
            queue.addAll(generators.getTransforms());

            for (Transform g : generators.getTransforms()) {  // Makes copies of fundamental domain by using generators
                Point3D genRef = g.transform(refPointHyperbolic);
                if (seen.insert(fDomain, genRef)) {    // Checks whether point "genRef" is in OctTree "seen". Adds it if not.
                    Group group2 = JavaFXUtils.copyFundamentalDomain(fund);
                    group2.getTransforms().add(g);
                    group.getChildren().add(group2);
                }
            }

            while (true && queue.size() > 0 && queue.size() < 10000) {
                final Transform t = queue.poll(); // remove t from queue

                //t.transform(refPointHyperbolic).getZ() < 0.4 * (maxDist + 1)
                if (isResetHyperbolic() && t.transform(refPointHyperbolic).getZ() < 2) {
                    transformFDHyperbolic = t;
                    setResetHyperbolic(false);
                }

                for (Transform g : generators.getTransforms()) {
                    Transform tg = t.createConcatenation(g);
                    Point3D bpt = tg.transform(refPointHyperbolic);
                    if (seen.insert(fDomain, bpt) && bpt.getZ() < maxDist) {
                        Group group2 = JavaFXUtils.copyFundamentalDomain(fund);
                        group2.getTransforms().add(tg);
                        group.getChildren().add(group2);
                        queue.add(tg);
                    }

                    Transform gt = g.createConcatenation(t);
                    bpt = gt.transform(refPointHyperbolic);
                    if (seen.insert(fDomain, bpt) && bpt.getZ() < maxDist) {
                        Group group2 = JavaFXUtils.copyFundamentalDomain(fund);
                        group2.getTransforms().add(gt);
                        group.getChildren().add(group2);
                        queue.add(gt);
                    }
                }
            }
        }
    return group;
    }
//----------------------------------------------------------------------------------------------------------------------
    /**
     * create tiling in Euclidean case
     *
     * @param windowCorner
     * @param width
     * @param height
     * @return group
     */
    public Group createTilingEuclidean(boolean drawFundamentalDomainOnly, Point3D windowCorner, double width, double height, double dx, double dy) {

        //Calculation of point of reference:
        refPointEuclidean = fDomain.getChamberCenter3D(1);

        final Group group = new Group();
        final Group fund = FundamentalDomain.buildFundamentalDomain(ds, fDomain);


        int j = 0;

        if (makeCopyEuclidean(refPointEuclidean, windowCorner, width, height, dx, dy)) {
            Translate t = new Translate();
            fund.getTransforms().add(t);
            fund.setRotationAxis(refPointEuclidean);
            group.getChildren().addAll(fund);
            j++;
        }

        if (!drawFundamentalDomainOnly) {

            //Add all generators
            computeConstraintsAndGenerators();

            final QuadTree seen = new QuadTree();
            seen.insert(refPointEuclidean.getX(), refPointEuclidean.getY());

            final Queue<Transform> queue = new LinkedList<>();
            queue.addAll(generators.getTransforms());

            for (Transform g : generators.getTransforms()) {  // Makes copies of fundamental domain by using generators
                Point3D genRef = g.transform(refPointEuclidean);
                if (isInRangeEuclidean(genRef, windowCorner, width, height) && seen.insert(genRef.getX(), genRef.getY())) {    // Checks whether point "genRef" is in OctTree "seen". Adds it if not.
                    if (makeCopyEuclidean(genRef, windowCorner, width, height, dx, dy)) {
                        j++;
                        Group group2 = JavaFXUtils.copyFundamentalDomain(fund);
                        group2.getTransforms().add(g);
                        group2.setRotationAxis(genRef);
                        group.getChildren().add(group2);
                    }

                }
            }


            while (queue.size() > 0 && j < 1000) {
                final Transform t = queue.poll(); // remove t from queue

                // Transform t copies fundamental domain back into a range of 400 times 400
                if (isResetEuclidean() && windowCorner.getX()+100 < t.transform(refPointEuclidean).getX() &&
                        t.transform(refPointEuclidean).getX() < windowCorner.getX()+500 &&
                        windowCorner.getY()+100 < t.transform(refPointEuclidean).getY() &&
                        t.transform(refPointEuclidean).getY() < windowCorner.getY()+500) {
                    transformFDEuclidean = t;
                    setResetEuclidean(false);
                }

                for (Transform g : generators.getTransforms()) {
                    Transform tg = t.createConcatenation(g);
                    Point3D bpt = tg.transform(refPointEuclidean);

                    if (isInRangeEuclidean(bpt, windowCorner, width, height) && seen.insert(bpt.getX(), bpt.getY()) ) {
                        if (makeCopyEuclidean(bpt, windowCorner, width, height, dx, dy)) {
                            /*if (recycler.getChildren().size() > 0){
                                Node node = recycler.getChildren().get(0);
                                node.getTransforms().clear();

                                //group.getChildren().add(node);

                                Transform t1 = fund.getLocalToParentTransform();
                                System.out.println(t1);
                                double det = t1.getMxx()*t1.getMyy()-t1.getMxy()*t1.getMyx();
                                Affine t1Inverse = new Affine(t1.getMyy()/det, -t1.getMxy()/det, 0, 0, -t1.getMyx()/det, t1.getMxx()/det, 0, 0, 0, 0, 1, 0);
                                Point3D tr = t1Inverse.transform(t1.getTx(), t1.getTy(), t1.getTz());
                                t1Inverse.setTx(-tr.getX()); t1Inverse.setTy(-t1.getTy());

                                Transform t2 = node.getLocalToParentTransform();
                                //System.out.println(t2);

                                node.getTransforms().add(tg.createConcatenation(t2));
                                node.setRotationAxis(bpt);
                            }
                            else*/ {
                                j++;
                                Group group2 = JavaFXUtils.copyFundamentalDomain(fund);
                                group2.getTransforms().add(tg);
                                group2.setRotationAxis(bpt);
                                group.getChildren().add(group2);
                            }
                        }
                        queue.add(tg);
                    }

                    Transform gt = g.createConcatenation(t);
                    bpt = gt.transform(refPointEuclidean);

                    if (isInRangeEuclidean(bpt, windowCorner, width, height) && seen.insert(bpt.getX(), bpt.getY())) {
                        if (makeCopyEuclidean(bpt, windowCorner, width, height, dx, dy)) {
                            j++;
                            Group group2 = JavaFXUtils.copyFundamentalDomain(fund);
                            group2.getTransforms().add(gt);
                            group2.setRotationAxis(bpt);
                            group.getChildren().add(group2);
                        }
                        queue.add(gt);
                    }
                }
            }
            System.out.println("Number of copies: " + j);
        }
        return group;
    }

//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

    /**
     * Euclidean case: Shifts back fundamental domain if out of bounds
     * @param height
     * @param width
     * @param windowCorner
     * @return transform
     */
    public Transform calculateBackShiftEuclidean(Point3D windowCorner, double width, double height){

        //Add all generators
        computeConstraintsAndGenerators();

        final Queue<Transform> queue = new LinkedList<>();
        queue.addAll(generators.getTransforms());

        refPointEuclidean = fDomain.getChamberCenter3D(1);
        final QuadTree seen = new QuadTree();
        seen.insert(refPointEuclidean.getX(),refPointEuclidean.getY());

        Transform backShift = new Translate(), t;
        Point3D apt = refPointEuclidean, point;
        double d = apt.distance(windowCorner);

        while (!isInRangeEuclidean(apt, windowCorner, width, height)){ // The loop works as long as the copy of fDomain lies outside the valid range

            t = queue.poll(); // remove t from queue

            for (Transform g : generators.getTransforms()){

                Transform tg = t.createConcatenation(g);
                point = tg.transform(refPointEuclidean);

                if (seen.insert(point.getX(), point.getY())) { // Creates a tree of points lying in the copies of fDomain
                    if (point.distance(windowCorner) < d){ // Optimizes the choice of the transformation copying fDomain back to the valid range
                        d = point.distance(windowCorner);
                        backShift = tg;
                        apt = point;
                    }
                    queue.add(tg);
                }

                Transform gt = g.createConcatenation(t);
                point = gt.transform(refPointEuclidean);

                if (seen.insert(point.getX(), point.getY())) {
                    if (point.distance(windowCorner) < d){
                        d = point.distance(windowCorner);
                        backShift = gt;
                        apt = point;
                    }
                    queue.add(gt);
                }
            }
        }
        return backShift;
    }

//----------------------------------------------------------------------------------------------------------------------

    /**
     * Hyperbolic case: Transform shifting back fundamental domain if out of bounds
     * @param maxDist
     * @return transform
     */
    public Transform calculateBackShiftHyperbolic(double maxDist){

        //Add all generators
        computeConstraintsAndGenerators();

        final Queue<Transform> queue = new LinkedList<>();
        queue.addAll(generators.getTransforms());


        refPointHyperbolic = fDomain.getChamberCenter3D(1);
        final OctTree seen = new OctTree();
        seen.insert(fDomain, refPointHyperbolic);

        Transform backShift = new Translate(), t;
        Point3D apt = refPointHyperbolic, point;
        double d = apt.getZ();

        while (apt.getZ()/100 >= maxDist){ // The loop works as long as the copy of fDomain lies outside the valid range

            t = queue.poll(); // remove t from queue
            for (Transform g : generators.getTransforms()){

                Transform tg = t.createConcatenation(g);
                point = tg.transform(refPointHyperbolic);

                if (seen.insert(fDomain, point)) { // Creates a tree of points lying in the copies of fDomain
                    if (point.getZ() < d){ // Optimizes the choice of the transformation copying fDomain back to the valid range
                        d = point.getZ();
                        backShift = tg;
                        apt = point;
                    }
                    queue.add(tg);
                }

                Transform gt = g.createConcatenation(t);
                point = gt.transform(refPointHyperbolic);

                if (seen.insert(fDomain, point)) {
                    if (point.getZ() < d){
                        d = point.getZ();
                        backShift = gt;
                        apt = point;
                    }
                    queue.add(gt);
                }
            }
        }
        return backShift;
    }

//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------



    /**
     * straigthen all edges
     */
    public void straightenAllEdges() {
        for (int i = 1; i <= numbEdge; i++) {
            straightenEdge(i);
        }
    }

    /**
     * straighten a specific edge
     *
     * @param edge
     */
    public void straightenEdge(int edge) {
        int i;
        int[] a = new int[5];

        if (edge < 1 || edge > numbEdge)
            throw new RuntimeException(String.format("straighten_edge(edge=%d): edge not in 1..%d", edge, numbEdge));

        a[0] = edge2flag[edge];

        if (ds.getSi(0, a[0]) != a[0])
            a[1] = ds.getSi(0, a[0]);
        else
            a[1] = 0;
        if (ds.getSi(2, a[0]) != a[0] && ds.getSi(2, a[0]) != a[1])
            a[2] = ds.getSi(2, a[0]);
        else
            a[2] = 0;
        if (a[2] != 0 && ds.getSi(0, a[2]) != a[0] && ds.getSi(0, a[2]) != a[1] && ds.getSi(0, a[2]) != a[2])
            a[3] = ds.getSi(0, a[2]);
        else
            a[3] = 0;

        for (i = 0; i < 4; i++) {
            if (a[i] != 0) // todo: this is broken: we probably need to calculate 2D transforms
            {
                Point2D aPt = fDomain.getVertex(0, a[i]);
                Point2D bPt = fDomain.getVertex(0, ds.getSi(0, a[i]));
                if (fDomain.isBoundaryEdge(0, a[i])) {
                    try {
                        Transform transform = generators.get(0, a[i]);
                        bPt = transform.inverseTransform(bPt); // todo: this needs fixing because saved transforms are 3D

                    } catch (NonInvertibleTransformException e) {
                        throw new RuntimeException(e);
                    }
                }
                Point2D app = new Point2D(aPt.getX(), aPt.getY());
                Point2D bpp = new Point2D(bPt.getX(), bPt.getY());
                Point2D cpp = middle(fDomain.getGeometry(), app, bpp);

                fDomain.setVertex(cpp, 1, a[i]);

                cpp = middle(fDomain.getGeometry(), app, cpp);
                fDomain.setEdgeCenter(cpp, 2, a[i]);
            }
        }
    }


    /**
     * compute middle point
     *
     * @param p
     * @param q
     * @return middle
     */
    public static Point2D middle(FDomain.Geometry geometry, Point2D p, Point2D q) {
        double d;

        int sign = -1; // hyperbolic

        switch (geometry) {
            default:
            case Euclidean:
                d = 0.5;
                break;
            case Spherical:
                sign = 1; // spherical
            case Hyperbolic:
                d = 2 * (1 + sign * p.dotProduct(q));
                if (d <= 0) d = 0;
                else d = 1 / Math.sqrt(d);
                break;
        }
        return new Point2D(d * (p.getX() + q.getX()), d * (p.getY() + q.getY()));
    }

    public DSymbol getDSymbol() {
        return ds;
    }

    public FDomain.Geometry getGeometry() {
        return fDomain.getGeometry();
    }

    public FDomain getfDomain() {
        return fDomain;
    }

    public boolean isResetHyperbolic() { return resetHyperbolic; }
    public void setResetHyperbolic(boolean reset) {
        this.resetHyperbolic = reset;
    }

    public boolean isResetEuclidean() { return  resetEuclidean; }
    public void  setResetEuclidean(boolean reset) { this.resetEuclidean = reset; }


    /**
     * Euclidean case: Checks whether "point" is in valid range
     * @param point
     * @param windowCorner
     * @param width
     * @param height
     * @return
     */
    public boolean isInRangeEuclidean(Point3D point, Point3D windowCorner, double width, double height){
        // Adjust width and height for a range around visible window. Range has at least dimensions 600 times 600
        if (width >= 350){ width += 250; }
        else { width = 600; }

        if (height >= 350){ height += 250; }
        else { height = 600; }

        if (windowCorner.getX()-250 <= point.getX() && point.getX() <= windowCorner.getX()+width &&
            windowCorner.getY()-250 <= point.getY() && point.getY() <= windowCorner.getY()+height){
            return true;
        } else{ return false; }
    }

    /**
     * Euclidean case: Checks whether "point" is in visible window
     * @param point
     * @param windowCorner
     * @param width
     * @param height
     * @return
     */
    public boolean isInWindowEuclidean(Point3D point, Point3D windowCorner, double width, double height){ //Checks whether point is in visible window
        if (windowCorner.getX() <= point.getX() && point.getX() <= windowCorner.getX() + width &&
            windowCorner.getY() <= point.getY() && point.getY() <= windowCorner.getY() + height){
            return true;
        } else { return false;}
    }

    /**
     * Euclidean case: Decides whether to make copy or not when tiling is translated.
     * @param point
     * @param windowCorner
     * @param width
     * @param height
     * @param dx
     * @param dy
     * @return
     */
    private boolean makeCopyEuclidean(Point3D point, Point3D windowCorner, double width, double height, double dx, double dy){
        // Adjust width and height for a range around visible window. Range has at least dimensions 600 times 600
        if (width >= 350){ width += 250; }
        else { width = 600; }

        if (height >= 350){ height += 250; }
        else { height = 600; }

        double left = windowCorner.getX()-250, right = windowCorner.getX()+width, upper = windowCorner.getY()-250, lower = windowCorner.getY()+height;

        if ((dx == 0 && dy == 0) || (point.getX() < left+dx || point.getX() > right+dx || point.getY() > lower+dy || point.getY() < upper+dy)){
            return true;
        } else {return  false;}
    }

}