package orm.util;

import orm.annotation.Id;

import java.lang.reflect.Field;

public class IdField {
    private Field field;
    private String primaryKey;

    public IdField(Field field) {
        this.field = field;
        this.primaryKey = field.getName();
    }

    public String getName() {
        return primaryKey;
    }

    public Class<?> getType() {
        return field.getType();
    }

    public Field getField() {
        return this.field;
    }
}
