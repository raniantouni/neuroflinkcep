package core.parser.dictionary;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.persistence.ElementCollection;
import javax.persistence.Id;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Set;

public class OldINFOREDictionary implements Serializable {
    private final static long serialVersionUID = -9058417690405646489L;

    @SerializedName("dictionaryName")
    @Expose
    @Id
    @NotEmpty(message = "Dictionary name must not be empty.")
    private String dictionaryName;

    @SerializedName("operators")
    @Expose
    @NotEmpty(message = "Dictionary Operators name must not be empty.")
    @ElementCollection
    private Set<@Valid DictionaryOperator> operators = null;

    /**
     * No args constructor for use in serialization
     */
    public OldINFOREDictionary() {
    }

    public OldINFOREDictionary(String dictionaryName, Set<DictionaryOperator> operators) {
        super();
        this.dictionaryName = dictionaryName;
        this.operators = operators;
    }

    public String getDictionaryName() {
        return dictionaryName;
    }

    public boolean addOperator(DictionaryOperator dictionaryOperator) {
        return operators.add(dictionaryOperator);
    }

    public Set<DictionaryOperator> getOperators() {
        return operators;
    }

    public boolean deleteOp(String class_key) {
        return this.operators.removeIf(f -> f.getClassKey().equals(class_key));
    }
}

