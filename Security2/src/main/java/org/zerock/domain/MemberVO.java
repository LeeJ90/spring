package org.zerock.domain;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class MemberVO {

    private String userid;
    private String userpw;
    private String userName;
    private String enabled;

    private Date regData;
    private Date updateDate;
    private List<AuthVO> authList;
}
