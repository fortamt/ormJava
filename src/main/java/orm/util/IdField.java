package orm.util;

import orm.annotation.Id;

import java.lang.reflect.Field;

public class IdField {
    private Field field;
    private Id primaryKey;

    public IdField(Field field) {
        this.field = field;
        this.primaryKey = this.field.getAnnotation(Id.class);
    }

    public String getName() {
        return primaryKey.name();
    }

    public Class<?> getType() {
        return field.getType();
    }

    public Field getField() {
        return this.field;
    }
}
