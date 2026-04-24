package me.marlonreal.allitems;

public enum FilterMode {

    ALL("All Items"),
    COLLECTED("Collected Only"),
    MISSING("Missing Only");

    private final String displayName;

    FilterMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public FilterMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}