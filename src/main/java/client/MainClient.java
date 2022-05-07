package client;

import client.model.entity.Zoo;
import orm.OrmManager;


import java.sql.SQLException;

public class MainClient {
    public static void main(String[] args) throws SQLException, IllegalAccessException {
        OrmManager ormManager = new OrmManager("H2schema");
        Zoo zoo4 = new Zoo("BBBB");
        ormManager.persist(zoo4);
        System.out.println(zoo4.getId());
    }
}
