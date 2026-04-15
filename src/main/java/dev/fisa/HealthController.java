package dev.fisa;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/version")
    public String version() {
//        System.out.println("테스트용입니다.");
        return "v4";
    }
}
