package com.quickmart.service;

import com.quickmart.dto.AddressRequest;
import com.quickmart.dto.AddressResponse;
import com.quickmart.entity.Address;
import com.quickmart.exception.ResourceNotFoundException;
import com.quickmart.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Every method takes the owner's customerId (from the token) and filters by it in the query.
// There is no "find by id" without a customer, so a missing owner filter doesn't compile.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;

    // Also used by the ADMIN endpoint, with the customer id from the URL instead of a token.
    public List<AddressResponse> list(Long customerId) {
        return addressRepository.findByCustomerIdOrderByIdAsc(customerId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    public AddressResponse get(Long customerId, Long addressId) {
        return AddressResponse.from(find(customerId, addressId));
    }

    @Transactional
    public AddressResponse create(Long customerId, AddressRequest req) {
        if (req.isDefault()) {
            // Same transaction as the insert: if the insert fails, the old default comes back.
            addressRepository.clearDefault(customerId);
        }
        Address saved = addressRepository.save(new Address(customerId, req.label(), req.line1(),
                req.city(), req.pincode(), req.isDefault()));
        return AddressResponse.from(saved);
    }

    @Transactional
    public void delete(Long customerId, Long addressId) {
        addressRepository.delete(find(customerId, addressId));
    }

    // Someone else's address and a missing one look identical: 404.
    private Address find(Long customerId, Long addressId) {
        return addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Address " + addressId + " not found"));
    }
}
