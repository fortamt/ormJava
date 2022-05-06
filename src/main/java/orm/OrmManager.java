package orm;

import client.model.entity.Animal;
import client.model.entity.Zoo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class OrmManager {

    AtomicLong idGenerator = new AtomicLong(0L);
    Connection connection;

    public OrmManager(String schemaName) {
        // using schemaName as a key find in
        // property file the configuration to connect to DB
        Properties properties = new Properties();
        try (InputStream is = ClassLoader.getSystemResourceAsStream("orm.properties")){
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
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (Zoo.class.equals(metamodel.getClassName())) {
                ColumnField columnField = metamodel.getColumns().get(0);
                Field field = columnField.getField();
                field.setAccessible(true);
                Object value = field.get(t);
                statement.setString(1, (String) value);
                Statement statementForResultSet = connection.createStatement();
                ResultSet rs = statementForResultSet.executeQuery("select max(id) from Zoo");
                rs.next();
                IdField idField = metamodel.getPrimaryKey();
                Field field1 = idField.getField();
                field1.setAccessible(true);
                field1.set(t, Long.valueOf(rs.getInt(1)));
            } else if (Animal.class.equals(metamodel.getClassName())) {
                //TODO implement serializing class Animnal to database
            }
            statement.executeUpdate();
        }
    }


    public void merge(Object objectToSave) {
        // save the object state into the DB table at
        // the row that has PK = object id (field marked as @Id)
    }

    public <T> T load(Object id, Class<T> entityClass) {
        // from DB find the row with PK = id in the table
        // where the objects of given type reside
        return null;
    }

    public void update(Object obj) {
        // go to DB to table = obj.getClass at PK = obj id
        // and set the fields of the obj <= data from DB
    }

    public void registerEntities(Class<?>... entityClasses) {
        // prepare MetaInfo, create the tables in the DB
    }

    public void remove(Object entity) {
        // send delete to DB and set id to null
    }

    private void processSqlException(SQLException e) {
        e.printStackTrace();
    }

    private void processIoException(IOException e) {
        e.printStackTrace();
    }




}
