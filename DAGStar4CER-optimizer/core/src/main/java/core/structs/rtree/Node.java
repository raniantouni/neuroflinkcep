package core.structs.rtree;

import core.structs.rtree.geometry.Geometry;
import core.structs.rtree.geometry.HasGeometry;
import rx.Subscriber;
import rx.functions.Func1;

import java.util.List;

public interface Node<T, S extends Geometry> extends HasGeometry {

    List<Node<T, S>> add(Entry<? extends T, ? extends S> entry);

    NodeAndEntries<T, S> delete(Entry<? extends T, ? extends S> entry, boolean all);

    void search(Func1<? super Geometry, Boolean> condition, Subscriber<? super Entry<T, S>> subscriber);

    int count();

}
