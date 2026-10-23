package ru.practicum.moviehub.model;

public class Movie {
    private Integer id;
    private String title;
    private int year;

    public Movie(String title, int year) {
        this.title = title;
        this.year = year;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getYear() {
        return year;
    }
}