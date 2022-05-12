package orm.util;

import orm.annotation.Id;

import java.lang.reflect.Field;

public class IdField {
    private Field field;
    private String primaryKey;

    public IdField(Field field) {
        this.field = field;
        this.primaryKey = this.field.getAnnotation(Id.class).name().equals("") ? field.getName() : this.field.getAnnotation(Id.class).name();
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
