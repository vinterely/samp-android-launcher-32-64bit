package com.glory.launcher.network;

public class Movie {

    private String title;

    private String plot;
    private boolean expanded;

    public Movie(String title, String plot) {
        this.title = title;

        this.plot = plot;
        this.expanded = false;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }



    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "title='" + title + '\'' +

                ", plot='" + plot + '\'' +
                ", expanded=" + expanded +
                '}';
    }
}
