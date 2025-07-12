package core.structs;

public class Triple<X, Y, Z> {
    public final X _1;
    public final Y _2;
    public final Z _3;


    public Triple(X _1, Y _2, Z _3) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
    }

    @Override
    public String toString() {
        return "(" + _1 + "," + _2 + "," + _3 + ")";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;

        if (_1 != null ? !_1.equals(triple._1) : triple._1 != null) return false;
        if (_2 != null ? !_2.equals(triple._2) : triple._2 != null) return false;
        return _3 != null ? _3.equals(triple._3) : triple._3 == null;
    }

    @Override
    public int hashCode() {
        int result = _1 != null ? _1.hashCode() : 0;
        result = 31 * result + (_2 != null ? _2.hashCode() : 0);
        result = 31 * result + (_3 != null ? _3.hashCode() : 0);
        return result;
    }
}
