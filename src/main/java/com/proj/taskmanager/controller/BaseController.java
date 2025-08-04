package com.proj.taskmanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BaseController {
    @GetMapping("/welcome")
    public String welcomePage(Model model) {
        model.addAttribute("username", "Alice");
        return "welcome";
    }
}
