package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoucherServiceTest {

    private static final BigDecimal SUBTOTAL = new BigDecimal("200000.00");

    private VoucherRepository voucherRepository;
    private VoucherUsageRepository usageRepository;
    private VoucherService service;

    @BeforeEach
    void setUp() {
        voucherRepository = mock(VoucherRepository.class);
        usageRepository = mock(VoucherUsageRepository.class);
        service = new VoucherService(voucherRepository, usageRepository);

        when(voucherRepository.save(any(Voucher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------- validateForCheckout ----------------

    @Test
    void validateReturnsVoucherAndNormalizesCode() {
        Voucher voucher = validVoucher();
        when(voucherRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(voucher));

        Voucher result = service.validateForCheckout("  sale10 ", 1L, SUBTOTAL);

        assertThat(result).isSameAs(voucher);
    }

    @Test
    void validateRejectsUnknownCode() {
        when(voucherRepository.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateForCheckout("nope", 1L, SUBTOTAL))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher code is invalid");
    }

    @Test
    void validateRejectsInactiveVoucher() {
        Voucher voucher = validVoucher();
        voucher.setStatus(CatalogStatus.INACTIVE);
        when(voucherRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.validateForCheckout("SALE10", 1L, SUBTOTAL))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher is not active");
    }

    @Test
    void validateRejectsVoucherThatHasNotStarted() {
        Voucher voucher = validVoucher();
        voucher.setStartDate(Instant.now().plusSeconds(3600));
        voucher.setEndDate(Instant.now().plusSeconds(7200));
        when(voucherRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.validateForCheckout("SALE10", 1L, SUBTOTAL))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher is not yet valid");
    }

    @Test
    void validateRejectsExpiredVoucher() {
        Voucher voucher = validVoucher();
        voucher.setStartDate(Instant.now().minusSeconds(7200));
        voucher.setEndDate(Instant.now().minusSeconds(3600));
        when(voucherRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.validateForCheckout("SALE10", 1L, SUBTOTAL))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher has expired");
    }

    @Test
    void validateRejectsWhenUsageLimitReached() {
        Voucher voucher = validVoucher();
        voucher.setUsageLimit(10);
        voucher.setUsedCount(10);
        when(voucherRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.validateForCheckout("SALE10", 1L, SUBTOTAL))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher usage limit reached");
    }

    @Test
    void validateRejectsOrderBelowMinimumValue() {
        Voucher voucher = validVoucher();
        voucher.setMinOrderValue(new BigDecimal("300000.00"));
        when(voucherRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.validateForCheckout("SALE10", 1L, SUBTOTAL))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order value is below the voucher minimum (300000.00)");
    }

    @Test
    void validateRejectsVoucherAlreadyUsedByThisCustomer() {
        Voucher voucher = validVoucher();
        when(voucherRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(voucher));
        when(usageRepository.existsByVoucherIdAndUserId(7L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.validateForCheckout("SALE10", 1L, SUBTOTAL))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You have already used this voucher");
    }

    // ---------------- admin create / update ----------------

    @Test
    void createRejectsDuplicateCode() {
        when(voucherRepository.existsByCodeIgnoreCase("SALE10")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(
                "sale10", DiscountType.FIXED, "10000", null,
                Instant.now(), Instant.now().plusSeconds(3600)
        )))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher code already exists");

        verify(voucherRepository, never()).save(any());
    }

    @Test
    void createRejectsStartDateNotBeforeEndDate() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> service.create(request(
                "SALE10", DiscountType.FIXED, "10000", null,
                now, now.minusSeconds(60)
        )))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("startDate must be before endDate");
    }

    @Test
    void createRejectsPercentageOverOneHundred() {
        assertThatThrownBy(() -> service.create(request(
                "SALE10", DiscountType.PERCENTAGE, "101", null,
                Instant.now(), Instant.now().plusSeconds(3600)
        )))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Percentage discount must not exceed 100");
    }

    @Test
    void createNormalizesCodeAndDropsMaxDiscountForFixedVoucher() {
        VoucherResponse response = service.create(request(
                "  sale10 ", DiscountType.FIXED, "10000", "5000",
                Instant.now(), Instant.now().plusSeconds(3600)
        ));

        assertThat(response.code()).isEqualTo("SALE10");
        assertThat(response.maxDiscount()).isNull();
        assertThat(response.minOrderValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.usedCount()).isZero();
    }

    @Test
    void updateRejectsUsageLimitBelowAlreadyUsedCount() {
        Voucher voucher = validVoucher();
        voucher.setUsedCount(5);
        when(voucherRepository.findById(7L)).thenReturn(Optional.of(voucher));

        VoucherRequest request = new VoucherRequest(
                "SALE10", null, DiscountType.FIXED,
                new BigDecimal("10000"), null, null, 3,
                Instant.now(), Instant.now().plusSeconds(3600)
        );

        assertThatThrownBy(() -> service.update(7L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(
                        "usageLimit must not be lower than the number of times the voucher has been used"
                );
    }

    // ---------------- recordUsage ----------------

    @Test
    void recordUsageSavesUsageWhenCounterIncreased() {
        Voucher voucher = validVoucher();
        when(voucherRepository.increaseUsedCount(eq(7L), any(Instant.class))).thenReturn(1);

        service.recordUsage(voucher, new User("a@b.com", "hash", "A", null), new Order(),
                new BigDecimal("10000.00"));

        verify(usageRepository).save(any(VoucherUsage.class));
    }

    @Test
    void recordUsageRejectsWhenCounterCouldNotBeIncreased() {
        Voucher voucher = validVoucher();
        when(voucherRepository.increaseUsedCount(eq(7L), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> service.recordUsage(
                voucher, new User("a@b.com", "hash", "A", null), new Order(),
                new BigDecimal("10000.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher usage limit reached");

        verify(usageRepository, never()).save(any());
    }

    // ---------------- helpers ----------------

    private Voucher validVoucher() {
        Voucher voucher = new Voucher();
        ReflectionTestUtils.setField(voucher, "id", 7L);
        voucher.setCode("SALE10");
        voucher.setDiscountType(DiscountType.FIXED);
        voucher.setDiscountValue(new BigDecimal("10000.00"));
        voucher.setMinOrderValue(new BigDecimal("0.00"));
        voucher.setStartDate(Instant.now().minusSeconds(3600));
        voucher.setEndDate(Instant.now().plusSeconds(3600));
        voucher.setStatus(CatalogStatus.ACTIVE);
        return voucher;
    }

    private VoucherRequest request(
            String code,
            DiscountType type,
            String value,
            String maxDiscount,
            Instant start,
            Instant end
    ) {
        return new VoucherRequest(
                code,
                null,
                type,
                new BigDecimal(value),
                null,
                maxDiscount == null ? null : new BigDecimal(maxDiscount),
                null,
                start,
                end
        );
    }
}
