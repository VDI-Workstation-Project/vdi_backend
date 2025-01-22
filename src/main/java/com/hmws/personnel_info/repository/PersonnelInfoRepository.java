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
    boolean existsByFirstNameAndLastNameAndMobile(String firstName, String lastName, String mobile);

    Optional<PersonnelInfo> findByFirstNameAndLastNameAndMobile(String firstName, String lastName, String mobile);

    @Query("SELECT p.role FROM PersonnelInfo p WHERE p.firstName = :firstName AND p.lastName = :lastName AND p.mobile = :mobile")
    UserRole findRoleByFirstNameAndLastNameAndPhoneNumber(@Param("firstName") String firstName,
                                                          @Param("lastName") String lastName,
                                                          @Param("mobile") String mobile);

    @Query("SELECT p.department FROM PersonnelInfo p WHERE p.firstName = :firstName AND p.lastName = :lastName AND p.mobile = :mobile")
    UserRole findDepartmentByFirstNameAndLastNameAndPhoneNumber(@Param("firstName") String firstName,
                                                          @Param("lastName") String lastName,
                                                          @Param("mobile") String mobile);
}
