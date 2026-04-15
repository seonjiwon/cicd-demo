package dev.fisa;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    // 이것은 주석 입니다.
    @GetMapping("/version")
    public String version() {
        return "v4";
    }
}
