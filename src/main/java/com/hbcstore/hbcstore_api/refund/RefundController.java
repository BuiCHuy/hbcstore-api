package com.hbcstore.hbcstore_api.refund;

import com.hbcstore.hbcstore_api.refund.dto.CreateRefundRequest;
import com.hbcstore.hbcstore_api.refund.dto.RefundResponse;
import com.hbcstore.hbcstore_api.refund.dto.RefundStatusUpdateRequest;
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
@RequestMapping("/api/refunds")
public class RefundController {
    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RefundResponse create(@Valid @RequestBody CreateRefundRequest request, Principal principal) {
        return refundService.create(principal == null ? null : principal.getName(), request);
    }

    @GetMapping("/mine")
    public List<RefundResponse> getMine(Principal principal) {
        return refundService.getMine(principal == null ? null : principal.getName());
    }

    @GetMapping
    public List<RefundResponse> getAll(Principal principal) {
        return refundService.getAllAdmin(principal == null ? null : principal.getName());
    }

    @PatchMapping("/{id}/status")
    public RefundResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RefundStatusUpdateRequest request,
            Principal principal
    ) {
        return refundService.updateStatus(id, principal == null ? null : principal.getName(), request);
    }
}

