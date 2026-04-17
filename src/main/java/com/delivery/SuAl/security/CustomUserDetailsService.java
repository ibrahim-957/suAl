package com.delivery.SuAl.security;

import com.delivery.SuAl.model.enums.UserRole;
import com.delivery.SuAl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (isPhoneNumber(identifier)) {
            return userRepository.findByPhoneNumberAndRole(identifier, UserRole.CUSTOMER)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Customer not found with phone: " + identifier
                    ));
        } else {
            return userRepository.findFirstByEmailAndRoleNot(identifier, UserRole.CUSTOMER)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "User not found with email: " + identifier
                    ));
        }
    }

    private boolean isPhoneNumber(String identifier) {
        return identifier != null && identifier.replaceAll("^\\+", "").matches("\\d+");
    }
}