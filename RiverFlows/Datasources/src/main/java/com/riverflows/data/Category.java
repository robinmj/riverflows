package com.riverflows.data;

/**
 * Used for qualitatively categorizing readings (i.e low, med, flood)
 * Created by robin on 7/5/15.
 */
public class Category {

    private final String name;
    private final Double max;
    private final Double min;

    /**
     *
     * @param name cannot be null
     * @param max null if this is an open set (no upper limit)
     * @param min null if this is an open set (no lower limit). Cannot be null if max is null.
     */
    public Category(String name, Double max, Double min) {
        if(name == null) {
            throw new NullPointerException();
        }
        if(max == null && min == null) {
            throw new IllegalArgumentException();
        }
        this.name = name;
        this.max = max;
        this.min = min;
    }

    public String getName() {
        return name;
    }

    public Double getMax() {
        return max;
    }

    public Double getMin() {
        return min;
    }
}
