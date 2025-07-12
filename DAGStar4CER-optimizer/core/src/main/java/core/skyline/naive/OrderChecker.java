package core.skyline.naive;


/**
 * A basic interface to factor comparisons.
 *
 * @param <Individual> The type of individuals checked.
 */
public interface OrderChecker<Individual> {
    /**
     * Tell if both individuals can be ordered as i1 worst than i2.
     *
     * @return true if i1 is worst than i2, false otherwise
     */
    boolean canOrderAs(Individual i1, Individual i2);
}