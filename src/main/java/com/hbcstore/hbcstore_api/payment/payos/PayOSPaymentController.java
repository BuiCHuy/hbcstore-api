package com.hbcstore.hbcstore_api.payment.payos;

import com.hbcstore.hbcstore_api.payment.payos.dto.PayOSCreatePaymentRequest;
import com.hbcstore.hbcstore_api.payment.payos.dto.PayOSCreatePaymentResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/payos")
public class PayOSPaymentController {
    private final PayOSPaymentService payOSPaymentService;

    public PayOSPaymentController(PayOSPaymentService payOSPaymentService) {
        this.payOSPaymentService = payOSPaymentService;
    }

    @PostMapping("/create")
    public PayOSCreatePaymentResponse createPayment(
            @Valid @RequestBody PayOSCreatePaymentRequest request,
            Principal principal
    ) {
        return payOSPaymentService.createPayment(
                request.orderId(),
                principal == null ? null : principal.getName()
        );
    }

    @GetMapping("/return")
    public Map<String, String> paymentReturn(@RequestParam Map<String, String> payload) {
        return payOSPaymentService.handleReturn(payload);
    }

    @PostMapping("/webhook")
    public Map<String, String> webhook(@RequestBody Map<String, Object> payload) {
        return payOSPaymentService.handleWebhook(payload);
    }

    @GetMapping("/settings")
    public Map<String, Object> settings() {
        return payOSPaymentService.getSettings();
    }
}
