package com.hmws.personnel_info.repository;

import com.hmws.personnel_info.domain.PersonnelInfo;
import com.hmws.usermgmt.constant.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonnelInfoRepository extends JpaRepository<PersonnelInfo, Long> {

    // 성, 이름, 휴대폰번호가 모두 일치하는 사람이 있는지 조회
    boolean existsByFirstNameAndLastNameAndPhoneNumber(String firstName, String lastName, String phoneNumber);

    Optional<PersonnelInfo> findByFirstNameAndLastNameAndPhoneNumber(String firstName, String lastName, String phoneNumber);

    @Query("SELECT p.role FROM PersonnelInfo p WHERE p.firstName = :firstName AND p.lastName = :lastName AND p.phoneNumber = :phoneNumber")
    UserRole findRoleByFirstNameAndLastNameAndPhoneNumber(@Param("firstName") String firstName,
                                                          @Param("lastName") String lastName,
                                                          @Param("phoneNumber") String phoneNumber);

    @Query("SELECT p.department FROM PersonnelInfo p WHERE p.firstName = :firstName AND p.lastName = :lastName AND p.phoneNumber = :phoneNumber")
    UserRole findDepartmentByFirstNameAndLastNameAndPhoneNumber(@Param("firstName") String firstName,
                                                          @Param("lastName") String lastName,
                                                          @Param("phoneNumber") String phoneNumber);
}
