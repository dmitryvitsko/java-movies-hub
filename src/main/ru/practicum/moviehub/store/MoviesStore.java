package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;

public class MoviesStore {
    private final List<Movie> movies = new ArrayList<>();
    private int nextId = 1;

    public List<Movie> getAll() {
        return new ArrayList<>(movies);
    }

    public Movie add(Movie movie) {
        movie.setId(nextId++);
        movies.add(movie);
        return movie;
    }

    public Movie getById(int id) {
        for (Movie movie : movies) {
            if (movie.getId() != null && movie.getId() == id) {
                return movie;
            }
        }
        return null;
    }

    public boolean delete(int id) {
        Movie movie = getById(id);
        if (movie != null) {
            movies.remove(movie);
            return true;
        }
        return false;
    }

    public List<Movie> getByYear(int year) {
        List<Movie> filtered = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie.getYear() == year) {
                filtered.add(movie);
            }
        }
        return filtered;
    }

    public void clear() {
        movies.clear();
    }
}