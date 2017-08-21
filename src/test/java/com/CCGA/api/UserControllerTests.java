package com.CCGA.api;

import com.CCGA.api.Models.*;
import com.CCGA.api.Repositorys.BookRepo;
import com.CCGA.api.Repositorys.MajorRepo;
import com.CCGA.api.Repositorys.SchoolRepo;
import com.CCGA.api.Repositorys.UserRepo;
import io.restassured.RestAssured;
import io.restassured.authentication.FormAuthConfig;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserControllerTests {
    private boolean setUpComplete = false;
    @Autowired
    UserRepo users;
    @Autowired
    SchoolRepo schools;
    @Autowired
    MajorRepo majors;
    @Autowired
    BookRepo books;
    @Autowired
    JdbcTemplate jdbc;

    @Before
    public void setUp() {
        if (setUpComplete){
            return;
        }
        User user1 = new User();
        Major major1 = new Major("Biology");
        majors.save(major1);

        List<Major> majorList = new ArrayList<>();
        majorList.add(major1);

        School school1 = new School("School", new Location(1, "street", "city", "state", 11111, 1111, new Float(1), new Float(1)), majorList);
        schools.save(school1);

        user1.setName("name");
        user1.setEmail("email");
        user1.setPassword("pass");
        user1.setMajor(major1);
        user1.setSchool(school1);
        users.save(user1);
        setUpComplete = true;
    }

    @Test
    public void testCanSave() {
        User foundUser = users.findByEmail("email");
        School school = schools.findByName("School");
        Major major = majors.findByName("Biology");
        assertNotNull(foundUser);
        assertEquals("Name should match", "name", "name");
        assertEquals("email should match", "email", "email");
        assertEquals("pass should match", "pass", "pass");
        assertEquals(school, foundUser.getSchool());
        assertEquals(major, foundUser.getMajor());


        get("/api/user/all").then().body("message", equalTo("success"));
        System.out.println("\n\n--------------Testing Books--------------\n");


        Major belongsTo = majors.findByName("Biology");
        Book book1 = new Book("bookName", "bookAuthor", "1234567890", "This is a description for book1", "Path to pictures of book", belongsTo);

        books.save(book1);


        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("email", "email");
        loginInfo.put("password", "pass");

        given()
            .contentType(JSON)
            .body(loginInfo)
            .when()
            .post("/api/user/login")
            .then()
            .statusCode(200);

        System.out.println("\nCan login\n");


        String sessionID = given().contentType(JSON).body(loginInfo).when().post("/api/user/login").sessionId();

        Map<String, Object> jsonBook = new HashMap<>();
        jsonBook.put("name", "bookName");

        given()
            .sessionId(sessionID)
            .contentType(JSON)
            .body(jsonBook)
            .when()
            .post("/api/user/book/add")
            .then()
            .body(equalTo("Successfully added book to collection"));


        Book exists = books.findByName("bookName");
        given()
            .sessionId(sessionID)
            .when()
            .get("/api/user/book/all")
            .then()
            .body(equalTo("[{\"bookID\":4,\"name\":\"bookName\",\"author\":\"bookAuthor\",\"isbn\":\"1234567890\",\"description\":\"This is a description for book1\",\"picture\":\"Path to pictures of book\",\"major\":{\"majorID\":1,\"name\":\"Biology\"}}]"));

        System.out.println("\nCan get list of books after logging in (should be empty)\n");
    }
}
