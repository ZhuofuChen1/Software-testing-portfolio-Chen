package com.example.cw1.service;

import com.example.cw1.dto.Drone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@Service
public class IlpDataService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("ilpBaseUrl")
    private String baseUrl;


    public Drone[] getDrones() {
        String url = baseUrl + "drones";
        try {
            System.out.println(">>> Calling ILP /drones: " + url);
            ResponseEntity<Drone[]> response =
                    restTemplate.getForEntity(url, Drone[].class);

            Drone[] body = response.getBody();
            int len = (body == null ? 0 : body.length);
            System.out.println(">>> /drones returned count: " + len);

            return body != null ? body : new Drone[0];
        } catch (Exception e) {
            e.printStackTrace();
            return new Drone[0];
        }
    }



}
