package com.hmws.portal_log_in_records.repository;

import com.hmws.portal_log_in_records.domain.PortalLogInRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PortalLoginRecordRepository extends JpaRepository<PortalLogInRecord, Long> {
}
