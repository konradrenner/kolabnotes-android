package org.kore.kolabnotes.android.content;

import org.kore.kolabnotes.android.Utils;

/**
 * Created by koni on 20.07.15.
 */
public final class NoteSorting {
    public enum Direction{
        ASC,DESC;
    }

    private final Direction direction;
    private final String columnName;

    public NoteSorting(Utils.SortingColumns columnName, Direction direction) {
        this.direction = direction;
        this.columnName = columnName.name();
    }

    public NoteSorting(){
        this(Utils.SortingColumns.lastModificationDate,Direction.DESC);
    }

    public Direction getDirection() {
        return direction;
    }

    public String getColumnName() {
        return columnName;
    }

    @Override
    public String toString() {
        return "Ordering{" +
                "direction=" + direction +
                ", columnName='" + columnName + '\'' +
                '}';
    }
}
