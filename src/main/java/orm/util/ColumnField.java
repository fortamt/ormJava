package orm.util;

import orm.annotation.Column;

import java.lang.reflect.Field;

public class ColumnField {

    private Field field;
    private String column;

    public ColumnField(Field field) {
        this.field = field;
        this.column = field.getName();
    }

    public String getName() {
        return column;
    }

    public Class<?> getType() {
        return field.getType();
    }

    public Field getField() {
        return this.field;
    }

}