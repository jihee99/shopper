package com.jihee.shopper.domain.user;

import com.jihee.shopper.domain.user.dto.AddressRequest;
import com.jihee.shopper.domain.user.dto.AddressResponse;
import com.jihee.shopper.domain.user.dto.UserResponse;
import com.jihee.shopper.domain.user.dto.UserUpdateRequest;
import com.jihee.shopper.domain.user.entity.Address;
import com.jihee.shopper.domain.user.entity.User;
import com.jihee.shopper.global.exception.CustomException;
import com.jihee.shopper.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 회원 서비스.
 *
 * <p>내 정보 조회·수정, 배송지 목록 조회·추가·삭제를 처리한다.
 * 모든 메서드는 인증된 사용자(userId)를 기반으로 동작한다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    // ── 내 정보 ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = findUserById(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = findUserById(userId);
        user.updateName(request.getName());
        return UserResponse.from(user);
    }

    // ── 배송지 ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        User user = findUserById(userId);

        // 첫 번째 배송지이거나 기본 배송지로 지정한 경우 → 기존 기본 배송지 해제
        if (request.isDefault()) {
            addressRepository.clearDefaultByUserId(userId);
        }

        Address address = Address.of(
                user,
                request.getName(),
                request.getRecipient(),
                request.getPhone(),
                request.getZipCode(),
                request.getAddress(),
                request.getAddressDetail(),
                request.isDefault()
        );

        addressRepository.save(address);
        return AddressResponse.from(address);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADDRESS_NOT_FOUND));
        addressRepository.delete(address);
    }

    // ── 내부 공용 ──────────────────────────────────────────────────────────────

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
