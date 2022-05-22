package orm.util;

import orm.annotation.Column;
import orm.annotation.ManyToOne;

import java.lang.reflect.Field;

public class ColumnField {

    private Field field;
    private String column;

    public ColumnField(Field field) {
        this.field = field;
        if (field.isAnnotationPresent(ManyToOne.class)) {
            this.column = field.getAnnotation(ManyToOne.class).name();
        } else if (field.isAnnotationPresent(Column.class)) {
            this.column = field.getAnnotation(Column.class).name();
        } else {
            this.column = field.getName();
        }
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