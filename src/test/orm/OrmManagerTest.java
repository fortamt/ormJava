package orm;

import client.model.entity.Zoo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class OrmManagerTest {
    @Test
    @DisplayName("Created object has null id")
    void test1() {
        Zoo zoo = new Zoo("California");
        assertNull(zoo.getId());
        zoo = null;
    }

    @Test
    @DisplayName("Created object has non - null id, after adding to DB")
    void test2() throws SQLException, IllegalAccessException {
        Zoo zoo = new Zoo("California");

        assertNull(zoo.getId());

        OrmManager h2 = new OrmManager("H2schema");
        h2.registerEntities(Zoo.class);
        h2.persist(zoo);

        assertEquals(1, zoo.getId());
        h2.remove(zoo);
        zoo = null;
    }

    @Test
    @DisplayName("Before saving DB empty")
    void test3() {

        OrmManager h2 = new OrmManager("H2schema");
        h2.registerEntities(Zoo.class);
        Zoo zoo = null;
        try{
            zoo = h2.load(1L, Zoo.class);
        } catch (Exception ignored){}
        assertNull(zoo);
    }

    @Test
    @DisplayName("After saving to DB we can find him by itself id")
    void test4() throws SQLException, IllegalAccessException {

        OrmManager h2 = new OrmManager("H2schema");
        h2.registerEntities(Zoo.class);
        Zoo zoo = null;
        try{
            zoo = h2.load(1L, Zoo.class);
        } catch (Exception ignored){}

        assertNull(zoo);

        zoo = new Zoo("California");
        h2.persist(zoo);
        Zoo newOne = h2.load(zoo.getId(), Zoo.class);

        assertEquals(zoo, newOne);
    }



}