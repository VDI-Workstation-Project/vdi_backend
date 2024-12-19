package com.hmws.vm_mgmt.sub_vms.repository;

import com.hmws.vm_mgmt.sub_vms.domain.SubVms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

@Repository
public interface SubVmsRepository extends JpaRepository<SubVms, Long> {

    Optional<SubVms> findByUserData_UserId(String userId);
}
