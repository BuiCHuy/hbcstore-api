package com.hbcstore.hbcstore_api.refund;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    List<RefundRequest> findByUser_EmailIgnoreCaseOrderByCreatedAtDesc(String email);
    List<RefundRequest> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    boolean existsByOrderIdAndStatusIn(Long orderId, List<RefundRequest.RefundStatus> statuses);
}
