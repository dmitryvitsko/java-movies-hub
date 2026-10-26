package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void setUp() {
        store.clear();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"), "Ожидается JSON-массив");
    }

    @Test
    void addMovie_withValidData_returnsCreated() throws Exception {
        String json = "{\"title\": \"Inception\", \"year\": 2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "При успешном добавлении должен возвращаться код 201");

        String body = resp.body();
        assertTrue(body.contains("\"id\":1"), "Ответ должен содержать присвоенный ID");
        assertTrue(body.contains("\"title\":\"Inception\""), "Ответ должен содержать название");
    }

    @Test
    void addMovie_withEmptyTitle_returns422() throws Exception {
        String json = "{\"title\": \"\", \"year\": 2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "Должен быть код 422 при пустом названии");
        assertTrue(resp.body().contains("название"), "Детали ошибки должны указывать на проблему с названием");
    }

    @Test
    void addMovie_withInvalidYear_returns422() throws Exception {
        String json = "{\"title\": \"Inception\", \"year\": 1800}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "Должен быть код 422 при неверном годе");
        assertTrue(resp.body().contains("год"), "Детали ошибки должны указывать на проблему с годом");
    }

    @Test
    void addMovie_withInvalidContentType_returns415() throws Exception {
        String json = "{\"title\": \"Inception\", \"year\": 2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode(), "Должен быть код 415 при неверном Content-Type");
    }

    @Test
    void addMovie_withWrongJson_returns400() throws Exception {
        String badJson = "{\"title\": \"Inception, \"year\": 2010";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(badJson))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "Должен быть код 400 при некорректном JSON");
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        Movie saved = store.add(new Movie("The Matrix", 1999));
        int id = saved.getId();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "Должен быть код 200 при успешном поиске");
        assertTrue(resp.body().contains("\"title\":\"The Matrix\""), "Тело ответа должно содержать название фильма");
    }

    @Test
    void getMovieById_whenNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/76567"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "Должен быть код 404, если фильм не найден");
    }

    @Test
    void getMovieById_withInvalidId_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/asfdgdg"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "Должен быть код 400 при некорректном ID");
    }

    @Test
    void deleteMovie_whenExists_returns204() throws Exception {
        Movie saved = store.add(new Movie("Interstellar", 2014));
        int id = saved.getId();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(), "Должен быть код 204 при успешном удалении");

        assertEquals(0, store.getAll().size(), "Фильм должен быть удален из хранилища");
    }

    @Test
    void deleteMovie_whenNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/35465"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "Должен быть код 404, если удаляемый фильм не найден");
    }

    @Test
    void deleteMovie_withInvalidId_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/asfsdhdfg"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "Должен быть код 400 при некорректном ID");
    }

    @Test
    void getMovies_byYear_returnsFilteredList() throws Exception {
        store.add(new Movie("Матрица", 1999));
        store.add(new Movie("Бойцовский клуб", 1999));
        store.add(new Movie("Начало", 2010));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1999"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "Должен быть код 200");
        String body = resp.body();
        assertTrue(body.contains("Матрица") && body.contains("Бойцовский клуб"), "Должны вернуться фильмы 1999 года");
        assertTrue(!body.contains("Начало"), "Не должны попасть в ответ фильмы с несоответствующим годом");
    }

    @Test
    void getMovies_byYear_whenNoMatch_returnsEmptyArray() throws Exception {
        store.add(new Movie("Начало", 2010));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2025"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "Должен быть код 200, если ничего не найдено");
        assertEquals("[]", resp.body().trim(), "Должен быть пустой массив");
    }

    @Test
    void getMovies_byYear_withInvalidFormat_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=afafdsg"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "Должен быть код 400, если введен неккоректный год");
    }

}