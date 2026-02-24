package com.jihee.shopper.domain.user;

import com.jihee.shopper.domain.user.dto.AddressRequest;
import com.jihee.shopper.domain.user.dto.AddressResponse;
import com.jihee.shopper.domain.user.dto.UserResponse;
import com.jihee.shopper.domain.user.dto.UserUpdateRequest;
import com.jihee.shopper.domain.user.entity.Address;
import com.jihee.shopper.domain.user.entity.User;
import com.jihee.shopper.global.exception.CustomException;
import com.jihee.shopper.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.createSocialUser("test@example.com", "Test User");
        userRepository.save(testUser);
    }

    // ── 내 정보 조회/수정 ───────────────────────────────────────────────

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMe_Success() {
        // when
        UserResponse response = userService.getMe(testUser.getId());

        // then
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("내 정보 수정 성공")
    void updateMe_Success() {
        // given
        UserUpdateRequest request = new UserUpdateRequest("Updated Name");

        // when
        UserResponse response = userService.updateMe(testUser.getId(), request);

        // then
        assertThat(response.getName()).isEqualTo("Updated Name");

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
    }

    // ── 배송지 관리 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("배송지 추가 성공")
    void addAddress_Success() {
        // given
        AddressRequest request = AddressRequest.builder()
                .name("집")
                .recipient("홍길동")
                .phone("010-1234-5678")
                .zipCode("12345")
                .address("서울특별시 강남구")
                .addressDetail("101동 101호")
                .isDefault(true)
                .build();

        // when
        AddressResponse response = userService.addAddress(testUser.getId(), request);

        // then
        assertThat(response.getName()).isEqualTo("집");
        assertThat(response.isDefault()).isTrue();
    }

    @Test
    @DisplayName("배송지 목록 조회 성공")
    void getAddresses_Success() {
        // given
        Address address1 = Address.of(
                testUser, "집", "홍길동", "010-1111-1111",
                "11111", "서울특별시 강남구", "101동 101호", true
        );
        Address address2 = Address.of(
                testUser, "회사", "홍길동", "010-2222-2222",
                "22222", "서울특별시 서초구", "201동 201호", false
        );
        addressRepository.save(address1);
        addressRepository.save(address2);

        // when
        List<AddressResponse> responses = userService.getAddresses(testUser.getId());

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting("name")
                .containsExactlyInAnyOrder("집", "회사");
    }

    @Test
    @DisplayName("배송지 삭제 성공")
    void deleteAddress_Success() {
        // given
        Address address = Address.of(
                testUser, "집", "홍길동", "010-1111-1111",
                "11111", "서울특별시 강남구", "101동 101호", true
        );
        addressRepository.save(address);

        // when
        userService.deleteAddress(testUser.getId(), address.getId());

        // then
        assertThat(addressRepository.findById(address.getId())).isEmpty();
    }
}
