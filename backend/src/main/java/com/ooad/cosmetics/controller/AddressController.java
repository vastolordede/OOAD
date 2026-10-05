package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.dto.address.AddressRequest;
import com.ooad.cosmetics.dto.address.AddressResponse;
import com.ooad.cosmetics.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> list() {
        return ApiResponse.success(addressService.listMine());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> create(
            @Valid @RequestBody AddressRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Address created",
                        addressService.create(request)
                ));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request
    ) {
        return ApiResponse.success(
                "Address updated",
                addressService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return ApiResponse.success("Address deleted");
    }

    @PutMapping("/{id}/default")
    public ApiResponse<AddressResponse> setDefault(@PathVariable Long id) {
        return ApiResponse.success(
                "Default address updated",
                addressService.setDefault(id)
        );
    }
}
