package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.dto.address.AddressRequest;
import com.ooad.cosmetics.dto.address.AddressResponse;
import com.ooad.cosmetics.entity.Address;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final CurrentUserService currentUserService;

    public AddressService(
            AddressRepository addressRepository,
            CurrentUserService currentUserService
    ) {
        this.addressRepository = addressRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listMine() {
        User user = currentUserService.requireCurrentUser();
        return addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(user.getId())
                .stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional
    public AddressResponse create(AddressRequest request) {
        User user = currentUserService.requireCurrentUser();

        Address address = new Address();
        address.setUser(user);
        apply(address, request);

        boolean shouldBeDefault =
                request.defaultAddress()
                        || addressRepository.countByUserId(user.getId()) == 0;

        if (shouldBeDefault) {
            clearDefault(user.getId());
            address.setDefaultAddress(true);
        }

        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(Long addressId, AddressRequest request) {
        User user = currentUserService.requireCurrentUser();
        Address address = requireOwned(addressId, user.getId());

        apply(address, request);

        if (request.defaultAddress() && !address.isDefaultAddress()) {
            clearDefault(user.getId());
            address.setDefaultAddress(true);
        } else if (!request.defaultAddress() && address.isDefaultAddress()) {
            address.setDefaultAddress(true);
        }

        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(Long addressId) {
        User user = currentUserService.requireCurrentUser();
        Address address = requireOwned(addressId, user.getId());
        boolean wasDefault = address.isDefaultAddress();

        addressRepository.delete(address);
        addressRepository.flush();

        if (wasDefault) {
            List<Address> remaining =
                    addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(user.getId());

            if (!remaining.isEmpty()) {
                Address replacement = remaining.get(0);
                replacement.setDefaultAddress(true);
            }
        }
    }

    @Transactional
    public AddressResponse setDefault(Long addressId) {
        User user = currentUserService.requireCurrentUser();
        Address address = requireOwned(addressId, user.getId());

        clearDefault(user.getId());
        address.setDefaultAddress(true);

        return AddressResponse.from(address);
    }

    private Address requireOwned(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Address", addressId)
                );
    }

    private void clearDefault(Long userId) {
        addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(userId)
                .forEach(address -> address.setDefaultAddress(false));
        addressRepository.flush();
    }

    private void apply(Address address, AddressRequest request) {
        address.setReceiverName(request.receiverName().trim());
        address.setPhone(request.phone().trim());
        address.setAddressLine(request.addressLine().trim());
        address.setWard(normalizeNullable(request.ward()));
        address.setDistrict(normalizeNullable(request.district()));
        address.setCity(request.city().trim());
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
