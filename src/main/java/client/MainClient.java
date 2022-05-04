package client;

import client.model.entity.Animal;
import client.model.entity.Zoo;
import orm.OrmManager;

public class MainClient {
    public static void main(String[] args) {
        var ormManager = new OrmManager("H2schema");
        var ormManagerPg = new OrmManager("PGschema");

        ormManager.rgisterEntities(Zoo.class, Animal.class);

        var zooOfNewYork = new Zoo("New York Zoo");
        System.out.println(zooOfNewYork.getId()); // null

        ormManager.persist(zooOfNewYork); // there is a row in DB table
        System.out.println(zooOfNewYork.getId()); // 1 (not null)

        long id = zooOfNewYork.getId();
        Zoo theZoo = ormManager.load(id, Zoo.class);

        zooOfNewYork.setName("Zoo of New York");
        ormManager.merge(zooOfNewYork);

        System.out.println(theZoo.getName().equals(
                zooOfNewYork.getName()
        )); // true if cache is used false if new object is loaded

        ormManager.update(theZoo);
        System.out.println(theZoo.getName().equals(
                zooOfNewYork.getName()
        )); // true
    }
}
