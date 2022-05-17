package client;

import client.model.entity.Animal;
import client.model.entity.Zoo;
import orm.OrmManager;
import orm.util.Metamodel;


import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;

public class MainClient {
    public static void main(String[] args) throws SQLException, IllegalAccessException, NoSuchFieldException {
        OrmManager orm = new OrmManager("H2schema");
        Zoo zoo = new Zoo("NewYork");
        Zoo zoo1 = new Zoo("California");
        Animal anim1 = new Animal("Bober", LocalDate.now());
        Animal anim2 = new Animal("Bear", LocalDate.of(2001, 1, 1));
        Animal anim3 = new Animal("Elephant", LocalDate.of(2002, 11, 23));
        Animal anim4 = new Animal("Lion", LocalDate.of(2006, 10, 2));
        zoo.addAnimal(anim1);
        zoo.addAnimal(anim2);
        zoo.addAnimal(anim3);
        zoo1.addAnimal(anim4);
        orm.registerEntities(Zoo.class, Animal.class);
        orm.persist(zoo);
        orm.persist(zoo1);
    }
}
