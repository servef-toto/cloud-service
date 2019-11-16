package com.cloud.user.dao;

import com.cloud.model.user.model.MerInvetoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StockDao {

    @Select("select * from mer_invetory t where t.mer_uid = #{id}")
    MerInvetoryEntity findById(Long id);

    /**
     *
     * @param productInventory
     */
    void updateProductInventory(MerInvetoryEntity productInventory);
}
