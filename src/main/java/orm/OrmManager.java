package orm;


import orm.Exceptions.ObjectNotFoundException;
import orm.annotation.ManyToOne;
import orm.annotation.OneToMany;
import orm.util.ColumnField;
import orm.util.Metamodel;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class OrmManager {

    Map<Class<?>, Map<Object, Object>> cache = new HashMap<>();

    public String getCache() {
        return cache.toString();
    }

    Connection connection;

    public OrmManager(String schemaName) {
        // using schemaName as a key find in
        // property file the configuration to connect to DB
        Properties properties = new Properties();
        try (InputStream is = ClassLoader.getSystemResourceAsStream("orm.properties")) {
            properties.load(is);
        } catch (IOException e) {
            processIoException(e);
        }
        String jdbcUrl = properties.getProperty(schemaName + ".dburl");
        String userName = properties.getProperty(schemaName + ".username");
        String password = properties.getProperty(schemaName + ".password");
        try {
            this.connection = DriverManager.getConnection(jdbcUrl, userName, password);
        } catch (SQLException e) {
            processSqlException(e);
        }
    }


    public <T> void persist(T t) throws IllegalArgumentException, IllegalAccessException, SQLException, NoSuchFieldException {
        Metamodel metamodel = Metamodel.of(t.getClass());
        String sql = metamodel.buildInsertSqlRequest(); // building sql request like "insert into Zoo (name) values (?)"
        try (PreparedStatement statement = prepareStatementWith(sql).andParameters(t)) {
            statement.executeUpdate();
            setIdToObjectAfterPersisting(t, statement);
            if (metamodel.isOneToManyPresent()) {
                for (var list : getMapOneToMany(t).entrySet()) {
                    for (var el : list.getValue()) {
                        persist(el);
                    }
                }
            }
            if (!isPresentInCache(t.getClass(), getPrimaryKeyValue(t))) {
                putInCache(t);
            }
        }
    }

    private PreparedStatementWrapper prepareStatementWith(String sql) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        return new PreparedStatementWrapper(statement);
    }

    private class PreparedStatementWrapper {

        private PreparedStatement statement;

        public PreparedStatementWrapper(PreparedStatement statement) {
            this.statement = statement;
        }

        public <T> PreparedStatement andParameters(T t) throws SQLException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
            Metamodel metamodel = Metamodel.of(t.getClass());
            for (int columnIndex = 0; columnIndex < metamodel.getColumns().size(); columnIndex++) {
                ColumnField columnField = metamodel.getColumns().get(columnIndex);
                Class<?> fieldType = columnField.getType();
                Field field = columnField.getField();
                field.setAccessible(true);
                Object value = field.get(t);
                if (fieldType == int.class || fieldType == long.class) {
                    statement.setInt(columnIndex + 1, (int) value);
                } else if (fieldType == double.class || fieldType == float.class) {
                    statement.setFloat(columnIndex + 1, (float) value);
                } else if (fieldType == String.class) {
                    statement.setString(columnIndex + 1, (String) value);
                } else if (fieldType == LocalDate.class) {
                    LocalDate localDate = (LocalDate) value;
                    java.util.Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    statement.setDate(columnIndex + 1, new Date(date.getTime()));
                }
                if (field.isAnnotationPresent(ManyToOne.class)) {
                    for (var el : getListManyToOne(t)) {
                        if (el != null) {
                            Metamodel metamodelForManyToOne = Metamodel.of(el.getClass());
                            Field fieldForManyToOne = metamodelForManyToOne.getPrimaryKey().getField();
                            fieldForManyToOne.setAccessible(true);
                            Object idToInsert = fieldForManyToOne.get(el);
                            statement.setObject(columnIndex + 1, idToInsert);
                        } else {
                            statement.setNull(columnIndex + 1, 4); //4 is SQL type code for integer
                        }
                    }
                }
            }
            return statement;
        }

        public <T> PreparedStatement andParametersAndKey(T t) throws SQLException, IllegalArgumentException, IllegalAccessException {
            Metamodel metamodel = Metamodel.of(t.getClass());
            for (int columnIndex = 0; columnIndex < metamodel.getColumns().size(); columnIndex++) {
                ColumnField columnField = metamodel.getColumns().get(columnIndex);
                Class<?> fieldType = columnField.getType();
                Field field = columnField.getField();
                field.setAccessible(true);
                Object value = field.get(t);
                if (fieldType == int.class || fieldType == long.class) {
                    statement.setInt(columnIndex + 1, (int) value);
                } else if (fieldType == double.class || fieldType == float.class) {
                    statement.setFloat(columnIndex + 1, (float) value);
                } else if (fieldType == String.class) {
                    statement.setString(columnIndex + 1, (String) value);
                } else if (fieldType == LocalDate.class) {
                    LocalDate localDate = (LocalDate) value;
                    java.util.Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    statement.setDate(columnIndex + 1, new Date(date.getTime()));
                }
            }

            Object value = getPrimaryKeyValue(t);
            statement.setLong(metamodel.getColumns().size() + 1, (Long) value);
            return statement;
        }

        public PreparedStatement andPrimaryKey(Object primaryKey) throws SQLException {
            if (primaryKey.getClass() == Long.class) {
                statement.setLong(1, (Long) primaryKey);
            }
            return statement;
        }
    }

    private <T> void setIdToObjectAfterPersisting(T t, PreparedStatement ps) throws SQLException, IllegalAccessException {
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

    private <T> Map<String, List<?>> getMapOneToMany(T t) throws IllegalAccessException {
        Metamodel metamodel = Metamodel.of(t.getClass());
        Map<String, List<?>> listOfLists = new HashMap<>();
        for (var el : metamodel.getOneToManyColumns()) {
            el.getField().setAccessible(true);
            listOfLists.put(el.getName(), (List<?>) el.getField().get(t));
        }
        return listOfLists;
    }

    private <T> List<Object> getListManyToOne(T t) throws IllegalAccessException {
        Metamodel metamodel = Metamodel.of(t.getClass());
        List<Object> listOfLists = new ArrayList<>();
        for (var el : metamodel.getManyToOneColumns()) {
            el.getField().setAccessible(true);
            listOfLists.add(el.getField().get(t));
        }
        return listOfLists;
    }

    public void merge(Object objectToSave) throws IllegalAccessException {

        Metamodel metamodel = Metamodel.of(objectToSave.getClass());
        Map<String, List<?>> oneToManyList = getMapOneToMany(objectToSave);
        String sql = metamodel.buildMergeRequest();
        try (PreparedStatement statement = prepareStatementWith(sql).andParametersAndKey(objectToSave)) {
            statement.executeUpdate();
            if (metamodel.isOneToManyPresent()) {
                int number1 = 0;
                int number = 0;
                for (var el : oneToManyList.entrySet()) {
                    number1++;
                    if (number1 < oneToManyList.size()) {
                        for (var el1 : el.getValue()) {
                            if (number < el.getValue().size()) {
                                merge(el1);
                                number++;

                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            processSqlException(e);
        }
    }

    public <T> T load(Object id, Class<T> clss) {
        // from DB find the row with PK = id in the table
        // where the objects of given type reside
        var result = findInCache(clss, id);
        if (result.isPresent()) {
            return (T) result.get();
        }
        Metamodel metamodel = Metamodel.of(clss);
        String sql = metamodel.buildSelectByIdRequest();
        try (PreparedStatement statement = prepareStatementWith(sql).andPrimaryKey(id);
             ResultSet resultSet = statement.executeQuery()) {
            T t = buildInstanceFrom(clss, resultSet, id);
            return t;
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            e.printStackTrace();
        }
        throw new ObjectNotFoundException("Cannot load object");
    }

    private <T> T buildInstanceFrom(Class<T> clss, ResultSet resultSet, Object id) throws
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, SQLException {

        Metamodel metamodel = Metamodel.of(clss);

        T t = clss.getConstructor().newInstance();
        Field primaryKeyField = metamodel.getPrimaryKey().getField();
        String primaryKeyColumnName = metamodel.getPrimaryKey().getName();
        Class<?> primaryKeyType = primaryKeyField.getType();
        resultSet.next();
        setFieldValue(resultSet, t, primaryKeyField, primaryKeyColumnName, primaryKeyType);
        putInCache(t);

        for (ColumnField columnField : metamodel.getColumnsNew()) {
            Field field = columnField.getField();
            field.setAccessible(true);
            Class<?> columnType = columnField.getType();
            String columnName = columnField.getName();
            if (field.isAnnotationPresent(ManyToOne.class)) {
                setFieldValue(resultSet, t, field, columnName);
            } else if (field.isAnnotationPresent(OneToMany.class)) {
                setFieldValue(t, field, id);
            } else {
                setFieldValue(resultSet, t, field, columnName, columnType);
            }
        }
        return t;
    }

    private <T> void setFieldValue(T t, Field field, Object id) throws
            SQLException, IllegalAccessException {
        List<Object> resultList = new ArrayList<>();
        List<Long> idList = new ArrayList<>();
        Metamodel metamodel = Metamodel.of(getGenericTypeOfList(field.getName(), t.getClass()));
        for (ColumnField manyToOneColumn : metamodel.getManyToOneColumns()) {
            if (manyToOneColumn.getType() == t.getClass()) {
                String sql = metamodel.buildSelectByForeignKey(metamodel, manyToOneColumn.getField(), id);
                try (PreparedStatement innerStatement = connection.prepareStatement(sql);
                     ResultSet innerResultSet = innerStatement.executeQuery()) {
                    while (innerResultSet.next()) {
                        var temp = innerResultSet.getLong(1);
                        idList.add(temp);
                    }
                }
            }
        }
        for (Long el : idList) {
            resultList.add(load(el, metamodel.getClss()));
        }
        field.setAccessible(true);
        field.set(t, resultList);
    }

    private <T> void setFieldValue(ResultSet resultSet, T t, Field field, String columnName) throws
            SQLException, IllegalAccessException {
        var primaryKey = resultSet.getObject(columnName, Long.class);
        var key = load(primaryKey, field.getType());
        field.setAccessible(true);
        field.set(t, key);
    }

    private <T> void setFieldValue(ResultSet resultSet, T t, Field field, String columnName, Class<?> keyType) throws
            SQLException, IllegalAccessException {
        var key = resultSet.getObject(columnName, keyType);
        field.setAccessible(true);
        field.set(t, key);
    }

    private <T> void setFieldValue(Field field, T t, Object value) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(t, value);
    }

    public void update(Object obj) {
        // go to DB to table = obj.getClass at PK = obj id
        // and set the fields of the obj <= data from DB
        Metamodel metamodel = Metamodel.of(obj.getClass());
        try (PreparedStatement ps = connection.prepareStatement(metamodel.buildSelectByIdSqlRequest())) {
            Object idToSelect = getPrimaryKeyValue(obj);
            ps.setObject(1, idToSelect);
            ResultSet rs = ps.executeQuery();
            rs.next();
            for (var el : metamodel.getColumns()) {
                Class<?> fieldType = el.getType();
                Field field = el.getField();
                field.setAccessible(true);
                if (fieldType == int.class || fieldType == long.class) {
                    field.set(obj, rs.getInt(el.getName()));
                } else if (fieldType == float.class) {
                    field.set(obj, rs.getFloat(el.getName()));
                } else if (fieldType == double.class) {
                    field.set(obj, rs.getDouble(el.getName()));
                } else if (fieldType == String.class) {
                    field.set(obj, rs.getString(el.getName()));
                } else if (fieldType == LocalDate.class) {
                    Date dateSql = rs.getDate(el.getName());
                    LocalDate localDate = dateSql.toLocalDate();
                    field.set(obj, localDate);
                }
            }
        } catch (SQLException | IllegalAccessException throwables) {
            throwables.printStackTrace();
        }
    }

    public <T> boolean saveOrUpdate(T object) throws SQLException, IllegalAccessException, NoSuchFieldException {
        Object value = getPrimaryKeyValue(object);
        boolean result = false;
        if (value == null) {
            persist(object);
            result = true;
        } else {
            merge(object);
        }
        return result;
    }

    private Object getPrimaryKeyValue(Object object) throws IllegalAccessException {
        Metamodel metamodel = Metamodel.of(object.getClass());
        Field field = metamodel.getPrimaryKey().getField();
        field.setAccessible(true);
        return field.get(object);
    }

    public void registerEntities(Class<?>... entityClasses) {
        // prepare MetaInfo, create the tables in the DB
        for (Class<?> clss : entityClasses) {
            Metamodel metamodel = Metamodel.of(clss);
            String sql = metamodel.buildTableInDbRequest();
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException e) {
                processSqlException(e);
            }
        }
        for (Class<?> clss : entityClasses) {
            Metamodel metamodel = Metamodel.of(clss);
            String sql = metamodel.buildConstraintSqlRequest();
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException e) {
                processSqlException(e);
            }
        }
    }

    public <T> Optional<T> find(Class<T> entityClass, Object id) {
        try {
            var result = load(id, entityClass);
            return Optional.of(result);
        } catch (ObjectNotFoundException e) {
            return Optional.empty();
        }
    }

    public <T> Collection<T> findAll(Class<T> entityClass) {
        Collection<T> result = new ArrayList<>();
        List<Object> keys = new ArrayList<>();
        Metamodel metamodel = Metamodel.of(entityClass);
        try (PreparedStatement statement = connection.prepareStatement("select " + metamodel.getPrimaryKey().getName() + " from " + metamodel.getClassName())) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                var pKey = resultSet.getObject(metamodel.getPrimaryKey().getName(), metamodel.getPrimaryKey().getType());
                keys.add(pKey);
            }
        } catch (SQLException e) {
            processSqlException(e);
        }
        for (Object key : keys) {
            if (isPresentInCache(entityClass, key)) {
                result.add((T) findInCache(entityClass, key).get());
            } else {
                result.add(find(entityClass, key).get());
            }
        }
        return result;
    }

    public int count(Class<?> entityClass) {
        int result = 0;
        Metamodel metamodel = Metamodel.of(entityClass);
        try (PreparedStatement statement = connection.prepareStatement(metamodel.buildCountRowsRequest())) {
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            result = resultSet.getInt(1);
        } catch (SQLException e) {
            processSqlException(e);
        }
        return result;
    }

    public void remove(Object entity) throws IllegalAccessException {
        // send delete to DB and set id to null
        Metamodel metamodel = Metamodel.of(entity.getClass());
        if (isPresentInCache(entity.getClass(), getPrimaryKeyValue(entity))) {
            removeFromCache(entity);
        }
        if (metamodel.isOneToManyPresent()) {
            for (var el : getMapOneToMany(entity).entrySet()) {
                removeCascade(el.getValue());
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(metamodel.buildRemoveSqlRequest())) {
            Object idToRemove = getPrimaryKeyValue(entity);
            ps.setObject(1, idToRemove);
            setFieldValue(metamodel.getPrimaryKey().getField(), entity, null);
            if (metamodel.isManyToOnePresent()) {
                for (var manyToOneField : metamodel.getManyToOneColumns()) {
                    setFieldValue(manyToOneField.getField(), entity, null);
                }
            }
            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void removeCascade(List<?> list) throws IllegalAccessException {
        if (list.isEmpty()) {
            return;
        }
        Metamodel metamodel = Metamodel.of(list.get(0).getClass());
        for (var el : list) {
            if (isPresentInCache(el.getClass(), getPrimaryKeyValue(el))) {
                removeFromCache(el);
            }
            try (PreparedStatement ps = connection.prepareStatement(metamodel.buildRemoveSqlRequest())) {
                Object idToRemove = getPrimaryKeyValue(el);
                ps.setObject(1, idToRemove);
                setFieldValue(metamodel.getPrimaryKey().getField(), el, null);
                for (var manyToOneObj : metamodel.getManyToOneColumns()) {
                    setFieldValue(manyToOneObj.getField(), el, null);
                }
                ps.executeUpdate();
            } catch (SQLException throwables) {
                processSqlException(throwables);
            } catch (IllegalAccessException e) {
                processIAException(e);
            }
        }
    }

    private <T> Optional<T> findInCache(Class<?> clss, Object id) {
        try {
            return Optional.ofNullable((T) cache.get(clss).get(id));
        } catch (NullPointerException e) {
            return Optional.empty();
        }
    }

    public boolean isPresentInCache(Class<?> clss, Object id) {
        if (cache.size() == 0 || cache.get(clss) == null) {
            return false;
        }
        return cache.get(clss).containsKey(id);
    }

    private <T> boolean putInCache(T t) {
        Metamodel metamodel = Metamodel.of(t.getClass());
        ColumnField idField = metamodel.getPrimaryKey();
        Field field = idField.getField();
        field.setAccessible(true);
        Object key;
        try {
            key = field.get(t);
        } catch (IllegalAccessException e) {
            processIAException(e);
            return false;
        }
        cache.putIfAbsent(t.getClass(), new HashMap<>());
        cache.get(t.getClass()).put(key, t);
        return true;
    }

    private <T> boolean removeFromCache(T t) {
        if (t == null) {
            return false;
        }
        try {
            cache.get(t.getClass()).remove(getPrimaryKeyValue(t));
        } catch (IllegalAccessException e) {
            processIAException(e);
            return false;
        }
        return true;
    }


    private Class<?> getGenericTypeOfList(String fieldName, Class<?> clss) {
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

    private void processSqlException(SQLException e) {
        e.printStackTrace();
    }

    private void processIoException(IOException e) {
        e.printStackTrace();
    }

    private void processIAException(IllegalAccessException e) {
        e.printStackTrace();
    }


}