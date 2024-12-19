package com.hmws.global.authentication;

import com.hmws.usermgmt.domain.UserData;
import com.hmws.usermgmt.repository.UserDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserDataRepository userDataRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UserData userData = userDataRepository.findByUserId(userId).orElseThrow(()
                -> new UsernameNotFoundException("User not found: " + userId));

        return new UserDetailsImpl(userData);
    }

}
