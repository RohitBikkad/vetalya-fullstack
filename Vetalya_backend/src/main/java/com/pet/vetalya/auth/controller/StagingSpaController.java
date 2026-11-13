package com.pet.vetalya.auth.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serve React entry points for direct links and browser refreshes in the combined staging image. */
@Controller
@ConditionalOnProperty(name = "app.static-ui-enabled", havingValue = "true")
public class StagingSpaController {
    @GetMapping({"/owner/login", "/owner/register", "/vet/login", "/vet/register", "/login"})
    public String app() { return "forward:/index.html"; }
}
