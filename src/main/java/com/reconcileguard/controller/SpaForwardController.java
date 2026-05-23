package com.reconcileguard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardController {
    @RequestMapping({
            "/signin",
            "/signup",
            "/verify",
            "/reset",
            "/reset/**",
            "/app",
            "/app/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}

