package com.quickmart.controller;

import com.quickmart.dto.AddressResponse;
import com.quickmart.dto.OrderResponse;
import com.quickmart.dto.PaymentResponse;
import com.quickmart.service.AddressService;
import com.quickmart.service.OrderService;
import com.quickmart.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ADMIN read access to customer data. One URL prefix, one rule in SecurityConfig ("/api/admin/** -> ADMIN").
 *
 * Why separate endpoints instead of "if admin, skip the owner filter" inside /api/orders/{id}?
 * The customer endpoints then have exactly one code path, always filtered by the token's customer id.
 * An admin branch in there is one wrong condition away from showing every customer everyone's orders.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final AddressService addressService;

    @GetMapping("/orders/{orderId}")
    public OrderResponse order(@PathVariable Long orderId) {
        return orderService.getAny(orderId);
    }

    @GetMapping("/orders/{orderId}/payments")
    public List<PaymentResponse> payments(@PathVariable Long orderId) {
        return paymentService.forAnyOrder(orderId);
    }

    @GetMapping("/customers/{customerId}/orders")
    public List<OrderResponse> customerOrders(@PathVariable Long customerId) {
        return orderService.history(customerId);
    }

    @GetMapping("/customers/{customerId}/addresses")
    public List<AddressResponse> customerAddresses(@PathVariable Long customerId) {
        return addressService.list(customerId);
    }
}
