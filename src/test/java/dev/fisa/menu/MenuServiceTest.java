package dev.fisa.menu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MenuServiceTest {

    private final MenuService menuService = new MenuService();

    @Test
    void recommend_returnsMenu() {
        Menu menu = menuService.recommend();
        assertThat(menu).isNotNull();
        assertThat(menu.getName()).isNotBlank();
    }

    @Test
    void getAllMenus_returnsNonEmptyList() {
        List<Menu> menus = menuService.getAllMenus();
        assertThat(menus).isNotEmpty();
    }

    @Test
    void getMenusByCategory_returnsFilteredList() {
        List<Menu> korean = menuService.getMenusByCategory("한식");
        assertThat(korean).isNotEmpty();
        assertThat(korean).allMatch(m -> m.getCategory().equals("한식"));
    }

    @Test
    void getMenusByCategory_returnsEmptyForUnknown() {
        List<Menu> result = menuService.getMenusByCategory("없는카테고리");
        assertThat(result).isEmpty();
    }
}
