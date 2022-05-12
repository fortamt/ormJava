package client;

import client.model.entity.Animal;
import client.model.entity.Zoo;
import orm.OrmManager;


import java.sql.SQLException;
import java.util.Collection;

public class MainClient {
    public static void main(String[] args) throws SQLException, IllegalAccessException {
        var ormManager = new OrmManager("H2schema");
//        var ormManagerPg = new OrmManager("PGschema");

        ormManager.registerEntities(Zoo.class, Animal.class);

        var zooOfNewYork = new Zoo("New York Zoo");
        System.out.println(zooOfNewYork.getId()); // null

        ormManager.persist(zooOfNewYork); // there is a row in DB table
        System.out.println(zooOfNewYork.getId()); // 1 (not null)

        long id = zooOfNewYork.getId();
        Zoo theZoo = ormManager.load(id, Zoo.class);
        System.out.println(theZoo);

        zooOfNewYork.setName("Zoo of New York");
        ormManager.merge(zooOfNewYork);

        System.out.println(theZoo.getName().equals(
                zooOfNewYork.getName()
        )); // true if cache is used false if new object is loaded

        ormManager.update(theZoo);
        System.out.println(theZoo.getName().equals(
                zooOfNewYork.getName()
        )); // true

        ormManager.remove(theZoo); // send delete to DB and set id to null
        System.out.println(theZoo.getId()); // null
    }

}
