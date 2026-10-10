package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.voucher.VoucherRequest;
import com.ooad.cosmetics.dto.voucher.VoucherResponse;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.DiscountType;
import com.ooad.cosmetics.entity.Order;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.entity.Voucher;
import com.ooad.cosmetics.entity.VoucherUsage;
import com.ooad.cosmetics.repository.VoucherRepository;
import com.ooad.cosmetics.repository.VoucherUsageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;

@Service
public class VoucherService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    public VoucherService(
            VoucherRepository voucherRepository,
            VoucherUsageRepository voucherUsageRepository
    ) {
        this.voucherRepository = voucherRepository;
        this.voucherUsageRepository = voucherUsageRepository;
    }

    // ------------------------------------------------------------------
    // V03: CRUD cho Admin
    // ------------------------------------------------------------------

    @Transactional
    public VoucherResponse create(VoucherRequest request) {
        validateRequest(request);

        String code = normalizeCode(request.code());
        if (voucherRepository.existsByCodeIgnoreCase(code)) {
            throw new BadRequestException("Voucher code already exists");
        }

        Voucher voucher = new Voucher();
        apply(voucher, request, code);
        return VoucherResponse.from(voucherRepository.save(voucher));
    }

    @Transactional
    public VoucherResponse update(Long id, VoucherRequest request) {
        validateRequest(request);

        Voucher voucher = requireVoucher(id);
        String code = normalizeCode(request.code());

        if (voucherRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BadRequestException("Voucher code already exists");
        }
        if (request.usageLimit() != null && request.usageLimit() < voucher.getUsedCount()) {
            throw new BadRequestException(
                    "usageLimit must not be lower than the number of times the voucher has been used"
            );
        }

        apply(voucher, request, code);
        return VoucherResponse.from(voucher);
    }

    @Transactional
    public VoucherResponse setStatus(Long id, CatalogStatus status) {
        Voucher voucher = requireVoucher(id);
        voucher.setStatus(status);
        return VoucherResponse.from(voucher);
    }

    @Transactional(readOnly = true)
    public VoucherResponse detail(Long id) {
        return VoucherResponse.from(requireVoucher(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<VoucherResponse> list(
            String q,
            CatalogStatus status,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        return PageResponse.from(
                voucherRepository.search(normalizeQuery(q), status, pageable)
                        .map(VoucherResponse::from)
        );
    }

    // ------------------------------------------------------------------
    // V04 - V08: kiểm tra voucher khi áp dụng vào đơn
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Voucher validateForCheckout(String rawCode, Long userId, BigDecimal subtotal) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(normalizeCode(rawCode))
                .orElseThrow(() -> new BadRequestException("Voucher code is invalid"));

        // V06
        if (voucher.getStatus() != CatalogStatus.ACTIVE) {
            throw new BadRequestException("Voucher is not active");
        }

        Instant now = Instant.now();
        if (now.isBefore(voucher.getStartDate())) {
            throw new BadRequestException("Voucher is not yet valid");
        }
        if (now.isAfter(voucher.getEndDate())) {
            throw new BadRequestException("Voucher has expired");
        }

        // V08
        if (voucher.getUsageLimit() != null
                && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new BadRequestException("Voucher usage limit reached");
        }

        // V07
        if (subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new BadRequestException(
                    "Order value is below the voucher minimum ("
                            + voucher.getMinOrderValue().toPlainString() + ")"
            );
        }

        if (voucherUsageRepository.existsByVoucherIdAndUserId(voucher.getId(), userId)) {
            throw new BadRequestException("You have already used this voucher");
        }

        return voucher;
    }

    // ------------------------------------------------------------------
    // V12: ghi nhận / hoàn lại lượt dùng. Chạy trong transaction của đơn hàng.
    // ------------------------------------------------------------------

    @Transactional
    public void recordUsage(Voucher voucher, User user, Order order, BigDecimal discountAmount) {
        int updated = voucherRepository.increaseUsedCount(voucher.getId(), Instant.now());
        if (updated == 0) {
            throw new BadRequestException("Voucher usage limit reached");
        }

        VoucherUsage usage = new VoucherUsage();
        usage.setVoucher(voucher);
        usage.setUser(user);
        usage.setOrder(order);
        usage.setDiscountAmount(discountAmount);
        voucherUsageRepository.save(usage);
    }

    /** Dùng khi hủy đơn (lượt sau): trả lại lượt dùng voucher của đơn đó. */
    @Transactional
    public void releaseUsage(Order order) {
        voucherUsageRepository.findByOrderId(order.getId()).ifPresent(usage -> {
            voucherRepository.decreaseUsedCount(usage.getVoucher().getId(), Instant.now());
            voucherUsageRepository.delete(usage);
        });
    }

    // ------------------------------------------------------------------

    private Voucher requireVoucher(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", id));
    }

    /** V05 và các ràng buộc giá trị giảm. */
    private void validateRequest(VoucherRequest request) {
        if (!request.startDate().isBefore(request.endDate())) {
            throw new BadRequestException("startDate must be before endDate");
        }
        if (request.discountType() == DiscountType.PERCENTAGE
                && request.discountValue().compareTo(HUNDRED) > 0) {
            throw new BadRequestException("Percentage discount must not exceed 100");
        }
    }

    private void apply(Voucher voucher, VoucherRequest request, String code) {
        voucher.setCode(code);
        voucher.setDescription(normalizeNullable(request.description()));
        voucher.setDiscountType(request.discountType());
        voucher.setDiscountValue(scale(request.discountValue()));
        voucher.setMinOrderValue(
                request.minOrderValue() == null
                        ? new BigDecimal("0.00")
                        : scale(request.minOrderValue())
        );
        // maxDiscount chỉ có nghĩa với voucher phần trăm.
        voucher.setMaxDiscount(
                request.discountType() == DiscountType.PERCENTAGE && request.maxDiscount() != null
                        ? scale(request.maxDiscount())
                        : null
        );
        voucher.setUsageLimit(request.usageLimit());
        voucher.setStartDate(request.startDate());
        voucher.setEndDate(request.endDate());
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String value = q.trim();
        return value.isEmpty() ? null : value;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
