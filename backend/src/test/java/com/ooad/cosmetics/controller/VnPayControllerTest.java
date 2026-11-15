package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.config.VnPayProperties;
import com.ooad.cosmetics.service.PaymentService;
import com.ooad.cosmetics.service.VnPayService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VnPayControllerTest {

    @Test
    void publicModesHideDisabledOrUnconfiguredGateways() {
        VnPayProperties config = new VnPayProperties();
        var controller = new VnPayController(
                mock(VnPayService.class), mock(PaymentService.class), config, false
        );

        var modes = controller.paymentModes().data();
        assertThat(modes.vnpayAvailable()).isFalse();
        assertThat(modes.simulationEnabled()).isFalse();
    }

    @Test
    void publicModesReportExplicitlyConfiguredOptions() {
        VnPayProperties config = new VnPayProperties();
        config.setTmnCode("SANDBOX_ID");
        config.setHashSecret("SANDBOX_SECRET");
        var controller = new VnPayController(
                mock(VnPayService.class), mock(PaymentService.class), config, true
        );

        var modes = controller.paymentModes().data();
        assertThat(modes.vnpayAvailable()).isTrue();
        assertThat(modes.simulationEnabled()).isTrue();
    }
}
