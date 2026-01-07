package com.example.cw1.dto;

import java.util.List;

public class RegionRequest {
    private Position position;
    private Region region;

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }


    public static class Region {
        private String name;
        private List<Position> vertices;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Position> getVertices() {
            return vertices;
        }

        public void setVertices(List<Position> vertices) {
            this.vertices = vertices;
        }
    }
}
