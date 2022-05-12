package orm;


import orm.util.ColumnField;
import orm.util.IdField;
import orm.util.Metamodel;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Properties;

public class OrmManager {

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


    public <T> void persist(T t) throws IllegalArgumentException, IllegalAccessException, SQLException {
        Metamodel metamodel = Metamodel.of(t.getClass());
        String sql = metamodel.buildInsertSqlRequest(); // building sql request like "insert into Zoo (name) values (?)"
        try (PreparedStatement statement = prepareStatementWith(sql).andParameters(t)) {
            statement.executeUpdate();
            setIdToObjectAfterPersisting(t, statement);
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

        public <T> PreparedStatement andParameters(T t) throws SQLException, IllegalArgumentException, IllegalAccessException {
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
                    java.util.Date date =  Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    statement.setDate(columnIndex + 1, new Date(date.getTime()));
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
                    java.util.Date date =  Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    statement.setDate(columnIndex + 1, new Date(date.getTime()));
                }
            }
            Field field = metamodel.getPrimaryKey().getField();
            field.setAccessible(true);
            Object value = field.get(t);
            statement.setLong(metamodel.getColumns().size()+1, (Long)value);
            return statement;
        }

        public PreparedStatement andPrimaryKey(Object primaryKey) throws SQLException {
            if (primaryKey.getClass() == Long.class) {
                statement.setLong(1, (Long)primaryKey);
            }
            return statement;
        }
    }

    private <T> void setIdToObjectAfterPersisting(T t, PreparedStatement ps) throws SQLException, IllegalAccessException {
        Metamodel metamodel = Metamodel.of(t.getClass());
        ResultSet resultSet = ps.getGeneratedKeys();
        if (resultSet.next()) {
            IdField idField = metamodel.getPrimaryKey();
            Field field1 = idField.getField();
            field1.setAccessible(true);
            field1.set(t, (long) resultSet.getInt(1));
        } else {
            throw new SQLException();
        }
    }

    public void merge(Object objectToSave) throws IllegalAccessException {
        // save the object state into the DB table at
        // the row that has PK = object id (field marked as @Id)
        Metamodel metamodel = Metamodel.of(objectToSave.getClass());
        String sql = metamodel.buildMergeRequest();
        try (PreparedStatement statement = prepareStatementWith(sql).andParametersAndKey(objectToSave)){
            statement.executeUpdate();
        } catch (SQLException e) {
            processSqlException(e);
        }
    }

    public <T> T load(Object id, Class<T> clss) {
        // from DB find the row with PK = id in the table
        // where the objects of given type reside
        Metamodel metamodel = Metamodel.of(clss);
        String sql = metamodel.buildSelectRequest();
        try (PreparedStatement statement = prepareStatementWith(sql).andPrimaryKey(id);
             ResultSet resultSet = statement.executeQuery()) {

            try {
                return buildInstanceFrom(clss, resultSet);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (SQLException e){
            processSqlException(e);
        }
        return null;
    }

    private <T> T buildInstanceFrom(Class<T> clss, ResultSet resultSet) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, SQLException {

        Metamodel metamodel = Metamodel.of(clss);

        T t = clss.getConstructor().newInstance();
        Field primaryKeyField = metamodel.getPrimaryKey().getField();
        String primaryKeyColumnName = metamodel.getPrimaryKey().getName();
        Class<?> primaryKeyType = primaryKeyField.getType();

        resultSet.next();
        setFieldValue(resultSet, t, primaryKeyField, primaryKeyColumnName, primaryKeyType);

        for (ColumnField columnField : metamodel.getColumns()) {
            Field field = columnField.getField();
            field.setAccessible(true);
            Class<?> columnType = columnField.getType();
            String columnName = columnField.getName();
            setFieldValue(resultSet, t, field, columnName, columnType);
        }

        return t;
    }

    private <T> void setFieldValue(ResultSet resultSet, T t, Field primaryKeyField, String primaryKeyColumnName, Class<?> primaryKeyType) throws SQLException, IllegalAccessException {
        var primaryKey = resultSet.getObject(primaryKeyColumnName, primaryKeyType);
        primaryKeyField.setAccessible(true);
        primaryKeyField.set(t, primaryKey);
    }

    public void update(Object obj) {
        // go to DB to table = obj.getClass at PK = obj id
        // and set the fields of the obj <= data from DB
        Metamodel metamodel = Metamodel.of(obj.getClass());
        try(PreparedStatement ps = connection.prepareStatement(metamodel.buildSelectByIdSqlRequest())) {
            IdField idField = metamodel.getPrimaryKey();
            Field fieldForId = idField.getField();
            fieldForId.setAccessible(true);
            Long idToSelect = (Long) fieldForId.get(obj);
            ps.setInt(1, Math.toIntExact(idToSelect));
            ResultSet rs = ps.executeQuery();
            rs.next();
            for(var el: metamodel.getColumns()) {
                ColumnField columnField = el;
                Class<?> fieldType = columnField.getType();
                Field field = columnField.getField();
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

    public void registerEntities(Class<?>... entityClasses) {
        // prepare MetaInfo, create the tables in the DB
        for(Class clss : entityClasses){
            Metamodel metamodel = Metamodel.of(clss);
            String sql = metamodel.buildTableInDbRequest();
            try (Statement statement = connection.createStatement()){
                statement.execute(sql);
            } catch (SQLException e) {
                processSqlException(e);
            }
        }
    }

    public void remove(Object entity) {
        // send delete to DB and set id to null
        Metamodel metamodel = Metamodel.of(entity.getClass());
        try(PreparedStatement st = connection.prepareStatement(metamodel.buildRemoveSqlRequest())) {
            IdField idField = metamodel.getPrimaryKey();
            Field field = idField.getField();
            field.setAccessible(true);
            Long idToRemove = (Long) field.get(entity);
            field.set(entity, null);
            st.setInt(1, Math.toIntExact(idToRemove));
        } catch (SQLException | IllegalAccessException throwables) {
            throwables.printStackTrace();
        }
    }

    private void processSqlException(SQLException e) {
        e.printStackTrace();
    }

    private void processIoException(IOException e) {
        e.printStackTrace();
    }


}
