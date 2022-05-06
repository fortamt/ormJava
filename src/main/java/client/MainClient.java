package client;

import client.model.entity.Animal;
import client.model.entity.Zoo;
import orm.Metamodel;
import orm.OrmManager;

import java.sql.SQLException;

public class MainClient {
    public static void main(String[] args) throws SQLException, IllegalAccessException {
        var ormManager = new OrmManager("H2schema");
        Zoo zoo3 = new Zoo("Reed");
        ormManager.persist(zoo3);
        System.out.println(zoo3.getId());

    }
}
