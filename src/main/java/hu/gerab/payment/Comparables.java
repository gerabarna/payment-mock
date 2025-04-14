package hu.gerab.payment;

public final class Comparables {

  private Comparables() {}

  /**
   * Equality comparison for {@link Comparable } classes based on {@link Comparable#compareTo}
   * method. This can be useful in case some class can compare as equal even if the equals() method
   * is not true. A good example for this is the {@link java.math.BigDecimal} class, where 0.2 ==
   * 0.200 as the scale of the 2 numbers are different, but '0.2'.compareTo('0.200')==0 as neither
   * is greater or lesser than the other.
   *
   * @param left the object to compare
   * @param right the 'other' object to compare
   * @param <T> any Comparable implementation class
   * @return true if either both instances are null, or non-null and compare as equal
   */
  public static <T extends Comparable<T>> boolean compareEquals(T left, T right) {
    return left == right || left != null && right != null && left.compareTo(right) == 0;
  }
}
