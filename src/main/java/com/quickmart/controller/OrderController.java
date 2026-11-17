package com.quickmart.controller;

import com.quickmart.dto.OrderResponse;
import com.quickmart.dto.PlaceOrderRequest;
import com.quickmart.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@Valid @RequestBody PlaceOrderRequest req) {
        return orderService.placeOrder(req);
    }

    // GET /api/orders?customerId=1
    @GetMapping
    public List<OrderResponse> history(@RequestParam Long customerId) {
        return orderService.history(customerId);
    }
}
