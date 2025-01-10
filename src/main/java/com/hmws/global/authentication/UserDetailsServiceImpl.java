package com.hmws.global.authentication;

import com.hmws.global.authentication.dto.AuthUserDto;
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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUserDto authUser = AuthUserDto.builder()
                .username(username)
                .build();

        return new UserDetailsImpl(authUser);
    }

}
