package com.mikiloz.meat.data;


// Created by Miguel Vera Belmonte on 04/02/2017.
public class Place {
    public double lat, lon;
    public String name;
    public Place(String name, double lat, double lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }
}
