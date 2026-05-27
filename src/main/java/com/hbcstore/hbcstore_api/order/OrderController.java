package com.hbcstore.hbcstore_api.order;

import com.hbcstore.hbcstore_api.order.dto.OrderResponse;
import com.hbcstore.hbcstore_api.order.dto.CreateOrderRequest;
import com.hbcstore.hbcstore_api.order.dto.OrderStatusRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> getAll() {
        return orderService.getAll();
    }

    @GetMapping("/mine")
    public List<OrderResponse> getMine(Principal principal) {
        return orderService.getMine(principal.getName());
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request, Principal principal) {
        return orderService.create(request, principal == null ? null : principal.getName());
    }

    @PostMapping("/guest-checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse guestCheckout(@Valid @RequestBody CreateOrderRequest request, Principal principal) {
        return orderService.create(request, principal == null ? null : principal.getName());
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        return orderService.updateStatus(id, request);
    }
}
