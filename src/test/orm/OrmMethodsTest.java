package orm;

import client.model.entity.Animal;
import client.model.entity.Zoo;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class OrmMethodsTest {

    private OrmManager ormManager;
    Zoo zoo;
    Animal animal1, animal2;


    @BeforeEach
    void setUp() {
        ormManager = new OrmManager("Test");
        ormManager.registerEntities(Zoo.class, Animal.class);
        zoo = new Zoo("Kharkiv");
        animal1 = new Animal("Bober", LocalDate.now());
        animal2 = new Animal("Osel", LocalDate.of(1996, 5, 8));
    }

    @AfterEach
    void tearDown() throws SQLException {
        ormManager.connection.close();
    }

    @Test
    @DisplayName("load test: if we loads object which was persisted, they must be equals and be cached")
    void test1() throws SQLException, NoSuchFieldException, IllegalAccessException {
        ormManager.persist(zoo);
        ormManager.cache.clear();
        assertFalse(ormManager.isPresentInCache(Zoo.class, zoo.getId()));
        Zoo zoo1 = ormManager.load(zoo.getId(), Zoo.class);
        assertEquals(zoo, zoo1);
        assertTrue(ormManager.isPresentInCache(Zoo.class, zoo.getId()));
    }

    @Test
    @DisplayName("load test: if Zoo has any Animals, they must be load too by eager")
    void test2() throws SQLException, NoSuchFieldException, IllegalAccessException {
        zoo.addAnimal(animal1);
        zoo.addAnimal(animal2);
        ormManager.persist(zoo);
        ormManager.cache.clear();
        assertTrue(ormManager.cache.isEmpty());
        Zoo zoo1 = ormManager.load(zoo.getId(), Zoo.class);
        for (Animal animal : zoo1.getAnimals()) {
            assertTrue(ormManager.isPresentInCache(Animal.class, animal.getId()));
        }
    }

    @Test
    @DisplayName("load test: if we loaded any Animal, Zoo must be loaded with all Animal")
    void test3() throws SQLException, NoSuchFieldException, IllegalAccessException {
        zoo.addAnimal(animal1);
        zoo.addAnimal(animal2);
        ormManager.persist(zoo);
        ormManager.cache.clear();
        assertTrue(ormManager.cache.isEmpty());
        Animal animal11 = ormManager.load(animal1.getId(), Animal.class);
        assertTrue(ormManager.isPresentInCache(Zoo.class, animal11.getZoo().getId()));
        assertTrue(ormManager.isPresentInCache(Animal.class, animal2.getId()));
    }
}