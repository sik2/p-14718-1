package com.back.boundedContext.market.in;

import com.back.boundedContext.market.app.MarketFacade;
import com.back.boundedContext.market.domain.Cart;
import com.back.boundedContext.market.domain.MarketMember;
import com.back.boundedContext.market.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@Slf4j
@Profile("!prod")
public class MarketDataInit {
    private static final int WAIT_SECONDS = 30;
    private static final int RETRY_INTERVAL_MS = 1000;

    private final MarketDataInit self;
    private final MarketFacade marketFacade;

    public MarketDataInit(
            @Lazy MarketDataInit self,
            MarketFacade marketFacade) {
        this.self = self;
        this.marketFacade = marketFacade;
    }

    @Bean
    @org.springframework.core.annotation.Order(3)
    public ApplicationRunner marketDataInitApplicationRunner() {
        return args -> {
            if (waitForMemberSync()) {
                self.makeBaseCarts();
                self.makeBaseProducts();
                self.makeBaseCartItems();
                self.makeBaseOrders();
                self.makeBasePaidOrders();
            }
        };
    }

    private boolean waitForMemberSync() {
        log.info("Waiting {}s for member sync...", WAIT_SECONDS);

        int maxRetries = WAIT_SECONDS * 1000 / RETRY_INTERVAL_MS;
        for (int i = 0; i < maxRetries; i++) {
            if (marketFacade.findMemberByUsername("user1").isPresent()) {
                log.info("Member sync completed. Proceeding with data init.");
                return true;
            }
            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.warn("Member sync timeout after {}s. Skipping data init.", WAIT_SECONDS);
        return false;
    }

    @Transactional
    public void makeBaseCarts() {
        var members = java.util.List.of("system", "holding", "admin", "user1", "user2", "user3");

        for (String username : members) {
            var memberOpt = marketFacade.findMemberByUsername(username);
            if (memberOpt.isEmpty()) continue;

            MarketMember member = memberOpt.get();
            if (marketFacade.findCartByBuyer(member).isPresent()) continue;

            marketFacade.createCart(member.toDto());
        }
    }

    @Transactional
    public void makeBaseProducts() {
        if (marketFacade.productsCount() > 0) return;

        var user1Opt = marketFacade.findMemberByUsername("user1");
        var user2Opt = marketFacade.findMemberByUsername("user2");
        var user3Opt = marketFacade.findMemberByUsername("user3");

        if (user1Opt.isEmpty() || user2Opt.isEmpty() || user3Opt.isEmpty()) {
            log.info("MarketMembers not found. Skipping product creation.");
            return;
        }

        MarketMember user1 = user1Opt.get();
        MarketMember user2 = user2Opt.get();
        MarketMember user3 = user3Opt.get();

        marketFacade.createProduct(user1, "post", 1, "상품1", "상품1 설명", 10_000, 10_000);
        marketFacade.createProduct(user1, "post", 2, "상품2", "상품2 설명", 15_000, 15_000);
        marketFacade.createProduct(user1, "post", 3, "상품3", "상품3 설명", 20_000, 20_000);
        marketFacade.createProduct(user2, "post", 4, "상품4", "상품4 설명", 25_000, 25_000);
        marketFacade.createProduct(user2, "post", 5, "상품5", "상품5 설명", 30_000, 30_000);
        marketFacade.createProduct(user3, "post", 6, "상품6", "상품6 설명", 35_000, 35_000);
    }

    @Transactional
    public void makeBaseCartItems() {
        var user1Opt = marketFacade.findMemberByUsername("user1");
        var user2Opt = marketFacade.findMemberByUsername("user2");
        var user3Opt = marketFacade.findMemberByUsername("user3");

        if (user1Opt.isEmpty() || user2Opt.isEmpty() || user3Opt.isEmpty()) return;

        var cart1Opt = marketFacade.findCartByBuyer(user1Opt.get());
        var cart2Opt = marketFacade.findCartByBuyer(user2Opt.get());
        var cart3Opt = marketFacade.findCartByBuyer(user3Opt.get());

        if (cart1Opt.isEmpty() || cart2Opt.isEmpty() || cart3Opt.isEmpty()) return;

        Cart cart1 = cart1Opt.get();
        Cart cart2 = cart2Opt.get();
        Cart cart3 = cart3Opt.get();

        var product1Opt = marketFacade.findProductById(1);
        var product2Opt = marketFacade.findProductById(2);
        var product3Opt = marketFacade.findProductById(3);
        var product4Opt = marketFacade.findProductById(4);

        if (product1Opt.isEmpty()) return;
        if (cart1.hasItems()) return;

        product1Opt.ifPresent(cart1::addItem);
        product2Opt.ifPresent(cart1::addItem);
        product3Opt.ifPresent(cart1::addItem);
        product4Opt.ifPresent(cart1::addItem);

        product1Opt.ifPresent(cart2::addItem);
        product2Opt.ifPresent(cart2::addItem);
        product3Opt.ifPresent(cart2::addItem);

        product1Opt.ifPresent(cart3::addItem);
        product2Opt.ifPresent(cart3::addItem);
    }

    @Transactional
    public void makeBaseOrders() {
        if (marketFacade.ordersCount() > 0) return;

        var user1Opt = marketFacade.findMemberByUsername("user1");
        var user2Opt = marketFacade.findMemberByUsername("user2");
        var user3Opt = marketFacade.findMemberByUsername("user3");

        if (user1Opt.isEmpty() || user2Opt.isEmpty() || user3Opt.isEmpty()) return;

        var cart1Opt = marketFacade.findCartByBuyer(user1Opt.get());
        var cart2Opt = marketFacade.findCartByBuyer(user2Opt.get());
        var cart3Opt = marketFacade.findCartByBuyer(user3Opt.get());

        if (cart1Opt.isEmpty() || cart2Opt.isEmpty() || cart3Opt.isEmpty()) return;

        Cart cart1 = cart1Opt.get();

        if (!cart1.hasItems()) return;

        marketFacade.createOrder(cart1);
        marketFacade.createOrder(cart2Opt.get());
        marketFacade.createOrder(cart3Opt.get());

        // 주문 생성 때문에 cart1이 비어있기 때문에 다시 아이템 추가
        marketFacade.findProductById(1).ifPresent(cart1::addItem);
        marketFacade.findProductById(2).ifPresent(cart1::addItem);
        marketFacade.findProductById(3).ifPresent(cart1::addItem);
        marketFacade.findProductById(4).ifPresent(cart1::addItem);
    }

    @Transactional
    public void makeBasePaidOrders() {
        var order1Opt = marketFacade.findOrderById(1);
        if (order1Opt.isEmpty()) return;

        Order order1 = order1Opt.get();
        if (order1.isPaid()) return;

        marketFacade.requestPayment(order1, 0);
    }
}
