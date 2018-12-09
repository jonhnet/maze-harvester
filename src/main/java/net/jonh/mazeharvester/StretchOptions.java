package net.jonh.mazeharvester;

public class StretchOptions {
  /** Ratio of desired path length to maze diagonal */
  private double stretch = 3.0;

  /**
   * How much to stretch path towards a wall on each stretch. 1.0 means add
   * the very longest path we can find; 0.8 means take an 80th-percentile
   * path.
   */
  private double deviationPercentile = 0.9;

  private StretchOptions() {
  }

  public double getStretch() { return stretch; }
  public double getDeviationPercentile() { return deviationPercentile; }

  public static Builder builder() { return new Builder(); }
  public static class Builder {
    private StretchOptions options = new StretchOptions();
    private Builder() {
    }
    public void setStretch(double stretch) {
      options.stretch = stretch;
    }
    public void setDeviationPercentile(double deviationPercentile) {
      options.deviationPercentile = deviationPercentile;
    }
    public StretchOptions build() {
      return options;
    }
  }
}
