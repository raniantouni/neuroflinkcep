package core.structs.rtree;

import core.structs.rtree.geometry.Geometry;
import core.structs.rtree.geometry.HasGeometry;
import core.structs.rtree.geometry.ListPair;
import core.structs.rtree.geometry.Rectangle;
import rx.functions.Func1;

import java.util.Comparator;
import java.util.List;

/**
 * Utility functions asociated with {@link Comparator}s, especially for use with
 * {@link Selector}s and {@link Splitter}s.
 */
public final class Comparators {

    public static final Comparator<ListPair<?>> overlapListPairComparator = toComparator(Functions.overlapListPair);
    /**
     * Compares the sum of the areas of two ListPairs.
     */
    public static final Comparator<ListPair<?>> areaPairComparator = (p1, p2) -> Float.compare(p1.areaSum(), p2.areaSum());

    private Comparators() {
        // prevent instantiation
    }

    /**
     * Returns a {@link Comparator} that is a normal Double comparator for the
     * total of the areas of overlap of the members of the list with the
     * rectangle r.
     *
     * @param <T>  type of geometry being compared
     * @param r    rectangle
     * @param list geometries to compare with the rectangle
     * @return the total of the areas of overlap of the geometries in the list
     * with the rectangle r
     */
    public static <T extends HasGeometry> Comparator<HasGeometry> overlapAreaComparator(final Rectangle r, final List<T> list) {
        return toComparator(Functions.overlapArea(r, list));
    }

    public static <T extends HasGeometry> Comparator<HasGeometry> areaIncreaseComparator(
            final Rectangle r) {
        return toComparator(Functions.areaIncrease(r));
    }

    public static Comparator<HasGeometry> areaComparator(final Rectangle r) {
        return (g1, g2) -> Float.compare(g1.geometry().mbr().add(r).area(), g2.geometry().mbr().add(r).area());
    }

    public static <R, T extends Comparable<T>> Comparator<R> toComparator(final Func1<R, T> function) {
        return Comparator.comparing(function::call);
    }

    @SafeVarargs
    public static <T> Comparator<T> compose(final Comparator<T>... comparators) {
        return (t1, t2) -> {
            for (Comparator<T> comparator : comparators) {
                int value = comparator.compare(t1, t2);
                if (value != 0)
                    return value;
            }
            return 0;
        };
    }

    /**
     * <p>
     * Returns a comparator that can be used to sort entries returned by search
     * methods. For example:
     * </p>
     * <p>
     * <code>search(100).toSortedList(ascendingDistance(r))</code>
     * </p>
     *
     * @param <T> the value type
     * @param <S> the entry type
     * @param r   rectangle to measure distance to
     * @return a comparator to sort by ascending distance from the rectangle
     */
    public static <T, S extends Geometry> Comparator<Entry<T, S>> ascendingDistance(final Rectangle r) {
        return Comparator.comparingDouble(e -> e.geometry().distance(r));
    }

}
