package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.dto.order.PaymentResponse;
import com.ooad.cosmetics.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PaymentControllerTest {

    @Test
    void disabledSimulationRejectsConfirmAndFailWithoutUpdatingPayments() {
        PaymentService service = mock(PaymentService.class);
        PaymentController controller = new PaymentController(service, false);

        var confirmed = controller.confirm(10L);
        var failed = controller.fail(10L);

        assertThat(confirmed.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(confirmed.getBody()).isNotNull();
        assertThat(confirmed.getBody().success()).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void explicitlyEnabledSimulationDelegatesToExistingService() {
        PaymentService service = mock(PaymentService.class);
        PaymentResponse response = mock(PaymentResponse.class);
        when(service.confirm(10L)).thenReturn(response);
        when(service.fail(10L)).thenReturn(response);
        PaymentController controller = new PaymentController(service, true);

        assertThat(controller.confirm(10L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.fail(10L).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).confirm(10L);
        verify(service).fail(10L);
    }
}
