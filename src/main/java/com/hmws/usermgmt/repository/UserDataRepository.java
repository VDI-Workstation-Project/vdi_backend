package com.hmws.usermgmt.repository;

import com.hmws.usermgmt.domain.UserData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDataRepository extends JpaRepository<UserData, Long> {
    Optional<UserData> findByUserId(String userId);
    Optional<UserData> findByEmail(String idWithDomain);
}
