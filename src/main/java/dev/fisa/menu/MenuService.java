package dev.fisa.menu;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class MenuService {

    private final Random random = new Random();

    private final List<Menu> menus = List.of(
            new Menu("김치찌개", "한식", 8000),
            new Menu("된장찌개", "한식", 8000),
            new Menu("비빔밥", "한식", 9000),
            new Menu("제육볶음", "한식", 9000),
            new Menu("불고기", "한식", 11000),
            new Menu("짜장면", "중식", 7000),
            new Menu("짬뽕", "중식", 8000),
            new Menu("탕수육", "중식", 15000),
            new Menu("초밥", "일식", 12000),
            new Menu("라멘", "일식", 10000),
            new Menu("돈카츠", "일식", 11000),
            new Menu("파스타", "양식", 13000),
            new Menu("피자", "양식", 18000),
            new Menu("햄버거", "양식", 9000),
            new Menu("쌀국수", "아시안", 10000),
            new Menu("팟타이", "아시안", 11000)
    );

    public Menu recommend() {
        return menus.get(random.nextInt(menus.size()));
    }

    public List<Menu> getAllMenus() {
        return menus;
    }

    public List<Menu> getMenusByCategory(String category) {
        return menus.stream()
                .filter(m -> m.getCategory().equals(category))
                .toList();
    }
}
