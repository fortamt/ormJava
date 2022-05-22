package orm;

import orm.util.ColumnField;
import orm.util.Metamodel;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrmUtilMethods {

    static <T> List<Object> getListManyToOne(T t) throws IllegalAccessException {
        Metamodel metamodel = Metamodel.of(t.getClass());
        List<Object> listOfLists = new ArrayList<>();
        for (var el : metamodel.getManyToOneColumns()) {
            el.getField().setAccessible(true);
            listOfLists.add(el.getField().get(t));
        }
        return listOfLists;
    }

    static <T> void setIdToObjectAfterPersisting(T t, PreparedStatement ps) throws SQLException, IllegalAccessException {
        Metamodel metamodel = Metamodel.of(t.getClass());
        ResultSet resultSet = ps.getGeneratedKeys();
        if (resultSet.next()) {
            ColumnField idField = metamodel.getPrimaryKey();
            Field field1 = idField.getField();
            field1.setAccessible(true);
            field1.set(t, (long) resultSet.getInt(1));
        } else {
            throw new SQLException();
        }
    }

    static <T> Map<String, List<?>> getMapOneToMany(T t) throws IllegalAccessException {
        Metamodel metamodel = Metamodel.of(t.getClass());
        Map<String, List<?>> listOfLists = new HashMap<>();
        for (var el : metamodel.getOneToManyColumns()) {
            el.getField().setAccessible(true);
            listOfLists.put(el.getName(), (List<?>) el.getField().get(t));
        }
        return listOfLists;
    }

    static Object getPrimaryKeyValue(Object object) throws IllegalAccessException {
        Metamodel metamodel = Metamodel.of(object.getClass());
        Field field = metamodel.getPrimaryKey().getField();
        field.setAccessible(true);
        return field.get(object);
    }

    static Class<?> getGenericTypeOfList(String fieldName, Class<?> clss) {
        Field listField;
        try {
            listField = clss.getDeclaredField(fieldName);
            listField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        ParameterizedType listType = (ParameterizedType) listField.getGenericType();
        return (Class<?>) listType.getActualTypeArguments()[0];
    }
}
