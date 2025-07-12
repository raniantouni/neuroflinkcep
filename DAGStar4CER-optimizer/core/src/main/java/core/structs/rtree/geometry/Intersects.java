package core.structs.rtree.geometry;

import rx.functions.Func2;

public class Intersects {
    public static final Func2<Rectangle, Circle, Boolean> rectangleIntersectsCircle = (rectangle, circle) -> circle.intersects(rectangle);
    public static final Func2<Point, Circle, Boolean> pointIntersectsCircle = (point, circle) -> circle.intersects(point);

    private Intersects() {
        // prevent instantiation
    }

}
