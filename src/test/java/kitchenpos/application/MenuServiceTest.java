package kitchenpos.application;

import kitchenpos.application.stub.MenuRepositoryStub;
import kitchenpos.application.stub.ProductRepositoryStub;
import kitchenpos.domain.*;
import kitchenpos.infra.PurgomalumClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static kitchenpos.fixture.KitchenposFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {
    @Mock
    private MenuGroupRepository menuGroupRepository;

    @Mock
    private PurgomalumClient purgomalumClient;

    private ProductRepository productRepository;

    private MenuRepository menuRepository;

    private Menu menu;

    @InjectMocks
    private MenuService menuService;

    @BeforeEach
    void setUp() {
        menuRepository = new MenuRepositoryStub();
        productRepository = new ProductRepositoryStub();
        menuService = new MenuService(menuRepository, menuGroupRepository, productRepository, purgomalumClient);

        menu = new Menu();
    }

    @DisplayName("메뉴를 등록할 수 있다.")
    @Test
    void create() {
        Menu request = createMenu();

        given(menuGroupRepository.findById(any())).willReturn(Optional.of(request.getMenuGroup()));
        when(purgomalumClient.containsProfanity(anyString())).thenReturn(false);

        Menu newMenu = menuService.create(request);

        assertThat(newMenu).isNotNull();
    }

    @DisplayName("메뉴의 가격이 입력되지 않으면 메뉴를 등록할 수 없다.")
    @ParameterizedTest
    @NullSource
    void emptyMenuPrice(BigDecimal price) {
        menu.setPrice(price);

        assertThatThrownBy(() -> menuService.create(menu))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("메뉴의 가격이 0 이하면 메뉴는 등록할 수 없다.")
    @Test
    void negativeInteger() {
        menu.setPrice(BigDecimal.valueOf(-1));

        assertThatThrownBy(() -> menuService.create(menu))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("메뉴의 가격은 메뉴 상품에 속해 있는 상품들의 총 가격의 합보다 클 수 없다.")
    @Test
    void sumPrice() {
        Menu request = createMenu();

        BigDecimal price = chickenProduct().getPrice().add(pastaProduct().getPrice()).add(BigDecimal.valueOf(1000));
        menu.setPrice(price);

        when(menuGroupRepository.findById(any())).thenReturn(Optional.of(request.getMenuGroup()));

        assertThatThrownBy(() -> menuService.create(menu))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("메뉴의 이름이 입력되지 않으면 메뉴를 등록할 수 없다.")
    @ParameterizedTest
    @NullSource
    void nullName(String name) {
        Menu menu = createMenu();
        menu.setName(name);

        when(menuGroupRepository.findById(any())).thenReturn(Optional.of(menu.getMenuGroup()));

        assertThatThrownBy(() -> menuService.create(menu))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("메뉴 이름에 나쁜말이 포함되어 있으면 메뉴를 등록할 수 없다.")
    @Test
    void badName() {
        Menu menu = createMenu();

        when(menuGroupRepository.findById(any())).thenReturn(Optional.of(menu.getMenuGroup()));
        when(purgomalumClient.containsProfanity(anyString())).thenReturn(true);

        assertThatThrownBy(() -> menuService.create(menu))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("메뉴 상품이 없으면 메뉴를 등록할 수 없다.")
    @Test
    void noMenuProduct() {
        Menu menu = createMenu();
        menu.setMenuProducts(new ArrayList<>());

        when(menuGroupRepository.findById(any())).thenReturn(Optional.of(menu.getMenuGroup()));

        assertThatThrownBy(() -> menuService.create(menu))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("메뉴 그룹을 입력해 주지 않으면 메뉴로 등록할 수 없다.")
    @Test
    void noMenuGroup() {
        Menu menu = createMenu();

        when(menuGroupRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.create(menu))
                .isInstanceOf(NoSuchElementException.class);
    }

    @DisplayName("메뉴 상품의 상품 수량은 0 이하 일 수 없다.")
    @Test
    void negativeMenuProductQuantity() {
        MenuGroup menuGroup = menuGroup();
        Product chicken = saveProduct(chickenProduct());
        Product pasta = saveProduct(pastaProduct());
        MenuProduct chickenMenuProduct = menuProduct(chicken, -1);
        MenuProduct pastaMenuProduct = menuProduct(pasta, 1);
        Menu menu = menuWithMenuProduct(menuGroup, chickenMenuProduct, pastaMenuProduct);

        when(menuGroupRepository.findById(any())).thenReturn(Optional.of(menuGroup));

        assertThatThrownBy(() -> menuService.create(menu))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("메뉴의 가격이 변경될 수 있다.")
    @Test
    void changePrice() {
        Menu menu = createMenu();
        menuRepository.save(menu);
        BigDecimal newPrice = menu.getPrice().subtract(BigDecimal.valueOf(1000));
        menu.setPrice(newPrice);

        Menu newPriceMenu = menuService.changePrice(menu.getId(), menu);

        assertThat(newPriceMenu.getPrice()).isEqualTo(newPrice);
    }

    @DisplayName("메뉴의 가격이 존재하지 않을 경우 가격을 변경할 수 없다.")
    @ParameterizedTest
    @NullSource
    void nullPrice(BigDecimal price) {
        menu.setId(UUID.randomUUID());
        menu.setPrice(price);

        assertThatThrownBy(() -> menuService.changePrice(menu.getId(), menu));
    }

    @DisplayName("메뉴의 가격이 0 보다 작을 경우 메뉴의 가격을 변경할 수 없다.")
    @Test
    void negativePrice() {
        menu.setId(UUID.randomUUID());
        menu.setPrice(BigDecimal.valueOf(-1));

        assertThatThrownBy(() -> menuService.changePrice(menu.getId(), menu));
    }

    @DisplayName("메뉴의 가격이 메뉴에 있는 모든 상품 가격의 총 합보다 클 경우 가격을 수정할 수 없다.")
    @Test
    void overPrice() {
        menu = createMenu();
        BigDecimal totalPrice = totalMenuProductPrice(menu.getMenuProducts());
        menu.setPrice(totalPrice.add(BigDecimal.valueOf(100)));

        assertThatThrownBy(() -> menuService.changePrice(menu.getId(), menu))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("메뉴의 전시상태를 전시중으로 변경할 수 있다.")
    @Test
    void display() {
        menu = menu(menuGroup(), chickenProduct(), pastaProduct());
        menu.setDisplayed(false);
        menuRepository.save(menu);

        Menu changedMenu = menuService.display(menu.getId());

        assertThat(changedMenu.isDisplayed()).isTrue();
    }

    @DisplayName("메뉴의 가격이 메뉴의 모든 상품의 가격의 합보다 크면 전시상태를 전시중으로 변경할 수 없다")
    @Test
    void doNotDisplay() {
        menu = menu(menuGroup(), chickenProduct(), pastaProduct());
        menu.setDisplayed(false);
        menu.setPrice(BigDecimal.valueOf(100000));
        menuRepository.save(menu);

        assertThatThrownBy(() -> menuService.display(menu.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("메뉴의 전시상태를 전시중지로 변경할 수 있다.")
    @Test
    void hide() {
        menu = menu(menuGroup(), chickenProduct(), pastaProduct());
        menu.setDisplayed(true);
        menuRepository.save(menu);

        Menu changedMenu = menuService.hide(menu.getId());

        assertThat(changedMenu.isDisplayed()).isFalse();
    }

    @DisplayName("존재하지 않는 메뉴의 전시상태를 전시중지로 변경할 수 있다.")
    @Test
    void NoSuchMenuHide() {
        assertThatThrownBy(() -> menuService.hide(UUID.randomUUID()))
                .isInstanceOf(NoSuchElementException.class);
    }

    private BigDecimal totalMenuProductPrice(List<MenuProduct> menuProducts) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (final MenuProduct menuProduct : menuProducts) {
            BigDecimal menuProductPrice = menuProduct.getProduct()
                    .getPrice()
                    .multiply(BigDecimal.valueOf(menuProduct.getQuantity()));
            totalPrice = totalPrice.add(menuProductPrice);
        }
        return totalPrice;
    }

    private Menu createMenu() {
        Product chicken = saveProduct(chickenProduct());
        Product pasta = saveProduct(pastaProduct());
        return menuRepository.save(menu(menuGroup(), chicken, pasta));
    }

    private Product saveProduct(Product product) {
        return productRepository.save(product);
    }
}