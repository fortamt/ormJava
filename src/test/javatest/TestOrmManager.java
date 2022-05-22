package javatest;

import client.model.entity.Animal;
import client.model.entity.Zoo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import orm.OrmManager;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


public class TestOrmManager {
    OrmManager ormManager;
    @BeforeEach
    void setConnection(){
        ormManager = new OrmManager("H2schema");
        ormManager.registerEntities(Zoo.class, Animal.class);
    }


    @Test
    @DisplayName("Test method merge")
    void test1() throws SQLException, NoSuchFieldException, IllegalAccessException {
        var zoo = new Zoo("myZoo");
        var bear = new Animal("Misha", LocalDate.of(2010, 5, 5));
        var parrot = new Animal("Red", LocalDate.of(2020, 2, 1));
        zoo.addAnimal(bear);
        zoo.addAnimal(parrot);
        ormManager.persist(zoo);
        zoo.setName("My Zoo");
        parrot.setName("Kesha");
        ormManager.merge(zoo);
        long id = zoo.getId();
        Zoo zoo1 = ormManager.load(id, Zoo.class);
        boolean result = zoo.equals(zoo1);
        assertTrue(result);
    }

    @Test
    @DisplayName("Test method count")
    void test2() throws SQLException, NoSuchFieldException, IllegalAccessException {
        var zoo1 = new Zoo("NY zoo");
        var zoo2 = new Zoo("Kharkiv zoo");
        var zoo3 = new Zoo("Kyiv zoo");
        ormManager.persist(zoo1);
        ormManager.persist(zoo2);
        ormManager.persist(zoo3);
        Collection<Zoo> collection = ormManager.findAll(Zoo.class);
        assertEquals(collection.size(), ormManager.count(Zoo.class));
    }

    @Test
    @DisplayName("Test method saveOrUpdate")
    void test3() throws SQLException, NoSuchFieldException, IllegalAccessException {
        var zoo1 = new Zoo("Magic Zoo");
        var animal1 = new Animal("Wolf", LocalDate.of(2016, 8, 10));
        var animal2 = new Animal("Bear", LocalDate.of(2010, 10, 12));
        var animal3 = new Animal("Fox", LocalDate.of(2021, 5, 4));
        zoo1.addAnimal(animal1);
        zoo1.addAnimal(animal2);
        zoo1.addAnimal(animal3);
        assertTrue(ormManager.saveOrUpdate(zoo1));
        animal2.setBirthDate(LocalDate.of(2011, 4, 12));
        assertFalse(ormManager.saveOrUpdate(zoo1));
    }

    @Test
    @DisplayName("Test method find")
    void test4() throws SQLException, NoSuchFieldException, IllegalAccessException {
        var zoo =  new Zoo ("Alabama zoo");
        var animal1 = new Animal("Elephant", LocalDate.of(2000, 5, 6));
        var animal2 = new Animal("Goat", LocalDate.of(2016, 8, 10));
        zoo.addAnimal(animal1);
        zoo.addAnimal(animal2);
        ormManager.persist(zoo);
        Optional<Zoo> zoo1 = ormManager.find(Zoo.class, zoo.getId());
        assertTrue(zoo1.isPresent());
    }

}
