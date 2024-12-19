package com.hmws.vm_mgmt.sub_vms.dto;

import com.hmws.usermgmt.domain.UserData;
import com.hmws.vm_mgmt.sub_vms.constants.VmStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class SubVmsDto {

    private Long subVmId;

    private String subVmName;

    private String subVmStatus;

    private UUID userNumber;
}
