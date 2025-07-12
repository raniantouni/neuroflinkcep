package optimizer.algorithm.flowoptimizer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@code Vertex}: A Vertex keeps a list of adjacent vertices {@code List<Vertex>}.
 */
public class Vertex {

    // The list of vertices connected with the vertex
    private final List<Vertex> adjVertices;

    // Properties of the vertex
    private int operatorId;   //RM Operator Name - Unique
    private int platform;
    private int site;

    public Vertex(int operatorId, int platform, int site) {
        this.operatorId = operatorId;
        this.platform = platform;
        this.site = site;
        this.adjVertices = new ArrayList<>();
    }

    /**
     * Vertex copy constructor with deep copy.
     *
     * @param u The vertex to copy.
     */
    public Vertex(Vertex u) {
        this(u.getOperatorId(), u.getPlatform(), u.getSite());
        for (Vertex v : u.getAdjVertices()) {
            this.adjVertices.add(new Vertex(v.operatorId, v.platform, v.site));
        }
    }

    public boolean addAdj(Vertex vertex) {
        return this.adjVertices.add(vertex);
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("0.0");//FIXME ...
        return operatorId + "(" + platform + "|" + site + "|" + df.format(0) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vertex vertex = (Vertex) o;
        return operatorId == vertex.operatorId && platform == vertex.platform && site == vertex.site && adjVertices.equals(vertex.adjVertices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adjVertices, operatorId, platform, site);
    }

    public List<Vertex> getAdjVertices() {
        return adjVertices;
    }

    public int getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(int operatorId) {
        this.operatorId = operatorId;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public int getSite() {
        return site;
    }

    public void setSite(int site) {
        this.site = site;
    }
}
