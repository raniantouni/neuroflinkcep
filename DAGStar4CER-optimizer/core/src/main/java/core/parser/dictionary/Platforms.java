package core.parser.dictionary;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import core.utils.JSONSingleton;

import javax.validation.Valid;
import java.io.Serializable;

public class Platforms implements Serializable {
    private final static long serialVersionUID = 6044389829993832633L;

    @SerializedName("spark")
    @Expose
    private @Valid Spark spark;

    @SerializedName("flink")
    @Expose
    private @Valid Flink flink;

    @SerializedName("akka")
    @Expose
    private @Valid Akka akka;

    /**
     * No args constructor for use in serialization
     */
    public Platforms() {
    }

    /**
     * @param flink
     * @param spark
     * @param akka
     */
    public Platforms(Spark spark, Flink flink, Akka akka) {
        super();
        this.spark = spark;
        this.flink = flink;
        this.akka = akka;
    }

    public Spark getSpark() {
        return spark;
    }

    public void setSpark(Spark spark) {
        this.spark = spark;
    }

    public Platforms withSpark(Spark spark) {
        this.spark = spark;
        return this;
    }

    public Flink getFlink() {
        return flink;
    }

    public void setFlink(Flink flink) {
        this.flink = flink;
    }

    public Platforms withFlink(Flink flink) {
        this.flink = flink;
        return this;
    }

    public Akka getAkka() {
        return akka;
    }

    public void setAkka(Akka akka) {
        this.akka = akka;
    }

    public Platforms withAkka(Akka akka) {
        this.akka = akka;
        return this;
    }

    @Override
    public String toString() {
        return JSONSingleton.toJson(this);
    }
}
