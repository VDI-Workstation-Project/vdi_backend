package com.hmws.vm_mgmt.sub_vms.domain;

import com.hmws.usermgmt.domain.UserData;
import com.hmws.vm_mgmt.sub_vms.constants.VmStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
public class SubVms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subVmId;

    private String subVmName;

    @Enumerated(EnumType.STRING)
    private VmStatus subVmStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userNumber")
    private UserData userData;


}
