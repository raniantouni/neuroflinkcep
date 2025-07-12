package core.structs.rtree;

import core.structs.rtree.geometry.HasGeometry;
import core.structs.rtree.geometry.ListPair;
import core.structs.rtree.geometry.Rectangle;
import rx.functions.Func1;

import java.util.List;

/**
 * Utility functions for making {@link Selector}s and {@link Splitter}s.
 */
public final class Functions {

    private Functions() {
        // prevent instantiation
    }

    public static final Func1<ListPair<? extends HasGeometry>, Double> overlapListPair = new Func1<ListPair<? extends HasGeometry>, Double>() {

        @Override
        public Double call(ListPair<? extends HasGeometry> pair) {
            return (double) pair.group1().geometry().mbr()
                    .intersectionArea(pair.group2().geometry().mbr());
        }
    };

    public static Func1<HasGeometry, Double> overlapArea(final Rectangle r,
                                                         final List<? extends HasGeometry> list) {
        return new Func1<HasGeometry, Double>() {

            @Override
            public Double call(HasGeometry g) {
                Rectangle gPlusR = g.geometry().mbr().add(r);
                double m = 0;
                for (HasGeometry other : list) {
                    if (other != g) {
                        m += gPlusR.intersectionArea(other.geometry().mbr());
                    }
                }
                return m;
            }
        };
    }

    public static Func1<HasGeometry, Double> areaIncrease(final Rectangle r) {
        return new Func1<HasGeometry, Double>() {
            @Override
            public Double call(HasGeometry g) {
                Rectangle gPlusR = g.geometry().mbr().add(r);
                return (double) (gPlusR.area() - g.geometry().mbr().area());
            }
        };
    }

}
