package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private final Gson gson;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            String path = ex.getRequestURI().getPath();
            String[] pathParts = path.split("/");

            if (pathParts.length == 2) {
                String query = ex.getRequestURI().getQuery();
                Integer yearParam = null;

                if (query != null) {
                    String[] pairs = query.split("&");

                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=");

                        if (keyValue.length == 2 && keyValue[0].equals("year")) {
                            try {
                                yearParam = Integer.parseInt(keyValue[1]);
                            } catch (NumberFormatException e) {
                                String error = gson.toJson(new ErrorResponse("Некорректный параметр запроса — 'year'"));
                                sendJson(ex, 400, error);
                                return;
                            }
                        }
                    }
                }
                if (yearParam != null) {
                    var filteredMovies = store.getByYear(yearParam);
                    sendJson(ex, 200, gson.toJson(filteredMovies));
                } else {
                    var movies = store.getAll();
                    sendJson(ex, 200, gson.toJson(movies));
                }
            } else if (pathParts.length == 3) {
                String idString = pathParts[2];
                try {
                    int id = Integer.parseInt(idString);
                    Movie movie = store.getById(id);

                    if (movie != null) {
                        sendJson(ex, 200, gson.toJson(movie));
                    } else {
                        String error = gson.toJson(new ErrorResponse("Фильм не найден"));
                        sendJson(ex, 404, error);
                    }
                } catch (NumberFormatException e) {
                    String error = gson.toJson(new ErrorResponse("Некорректный ID"));
                    sendJson(ex, 400, error);
                }
            } else {
                String error = gson.toJson(new ErrorResponse("Неверный путь"));
                sendJson(ex, 400, error);
            }
        } else if (method.equalsIgnoreCase("POST")) {

            String contentType = ex.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("application/json")) {
                String error = gson.toJson(new ErrorResponse("Ожидается application/json"));
                sendJson(ex, 415, error);
                return;
            }

            byte[] bodyBytes = ex.getRequestBody().readAllBytes();
            String bodyString = new String(bodyBytes, StandardCharsets.UTF_8);

            Movie movie;
            try {
                movie = gson.fromJson(bodyString, Movie.class);
            } catch (JsonSyntaxException e) {
                String error = gson.toJson(new ErrorResponse("Некорректный формат JSON"));
                sendJson(ex, 400, error);
                return;
            }

            List<String> validationErrors = new ArrayList<>();

            if (movie.getTitle() == null || movie.getTitle().isBlank()) {
                validationErrors.add("название не должно быть пустым");
            } else if (movie.getTitle().length() > 100) {
                validationErrors.add("название не должно быть длиннее 100 символов");
            }

            int maxYear = LocalDate.now().getYear() + 1;
            if (movie.getYear() < 1888 || movie.getYear() > maxYear) {
                validationErrors.add("год должен быть между 1888 и " + maxYear);
            }

            if (!validationErrors.isEmpty()) {
                ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", validationErrors);
                sendJson(ex, 422, gson.toJson(errorResponse));
                return;
            }

            Movie savedMovie = store.add(movie);
            sendJson(ex, 201, gson.toJson(savedMovie));

        } else if (method.equalsIgnoreCase("DELETE")) {
            String path = ex.getRequestURI().getPath();
            String[] pathParts = path.split("/");

            if (pathParts.length == 3) {
                String idString = pathParts[2];
                try {
                    int id = Integer.parseInt(idString);
                    boolean isDeleted = store.delete(id);

                    if (isDeleted) {
                        sendNoContent(ex);
                    } else {
                        String error = gson.toJson(new ErrorResponse("Фильм не найден"));
                        sendJson(ex, 404, error);
                    }
                } catch (NumberFormatException e) {
                    String error = gson.toJson(new ErrorResponse("Некорректный ID"));
                    sendJson(ex, 400, error);
                }
            } else {
                String error = gson.toJson(new ErrorResponse("Неверный путь для удаления"));
                sendJson(ex, 400, error);
            }

        } else {
            String error = gson.toJson(new ErrorResponse("Метод не поддерживается"));
            sendJson(ex, 405, error);
        }
    }
}