package com.pet.vetalya.auth.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Return the React document directly so container forwards cannot produce an empty successful response. */
@Controller
@ConditionalOnProperty(name = "app.static-ui-enabled", havingValue = "true")
public class StagingSpaController {
    @GetMapping({"/owner/login", "/owner/register", "/vet/login", "/vet/register", "/login"})
    public ResponseEntity<Resource> app() {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(new ClassPathResource("static/index.html"));
    }
}
