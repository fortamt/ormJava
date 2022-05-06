package client;

import client.model.entity.Animal;
import client.model.entity.Zoo;
import orm.OrmManager;

public class MainClient {
    public static void main(String[] args) {
        var ormManager = new OrmManager("H2schema");
//        Zoo zoo = new Zoo("NewYork");
//        ormManager.persist(zoo);
//        Zoo zoo1 = new Zoo("LosAngeles");
//        ormManager.persist(zoo1);
        Zoo zoo2 = new Zoo("LasVegas");
        ormManager.persist(zoo2);
        System.out.println(zoo2.getId());
    }
}
