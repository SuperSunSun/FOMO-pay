package com.example.fomopay.controller;

import com.example.fomopay.service.FomoPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fomopay")
public class FomoPayController {

    @Autowired
    private FomoPayService fomoPayService;

    @GetMapping("/test")
    public String testFomoPayIntegration() {
        try {
            return fomoPayService.testFomoPayIntegration();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}
