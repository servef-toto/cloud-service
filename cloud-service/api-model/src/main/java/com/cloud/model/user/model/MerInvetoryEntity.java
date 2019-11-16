package com.cloud.model.user.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class MerInvetoryEntity implements Serializable {

    private static final long serialVersionUID = 611197991672067628L;

    private Long id;
    private Long merUid;
    private Integer merCount;
    private Date createTime;
    private Date updateTime;

    public MerInvetoryEntity(Long productId, int l) {
        this.merUid = productId;
        this.merCount = l;
    }
}
