package core.skyline;


import core.structs.rtree.Entry;
import core.structs.rtree.RTree;
import core.structs.rtree.geometry.Geometries;
import core.structs.rtree.geometry.Point;

import java.util.ArrayList;
import java.util.List;

public class Utilities {
    public static RTree<Object, Point> constructRTree(RTree<Object, Point> rTree, List<Point> points) {
        for (Point point : points) {
            rTree = rTree.add(new Object(), point);
        }
        return rTree;
    }

    public static List<Point> entriesToPoints(final List<Entry<Object, Point>> entries) {
        List<Point> points = new ArrayList<>();
        if (!entries.isEmpty()) {
            for (Entry<Object, Point> entry : entries) {
                points.add(Geometries.point(entry.geometry().x(), entry.geometry().y()));
            }
        }

        return points;
    }
}
