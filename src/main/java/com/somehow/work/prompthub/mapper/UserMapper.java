package com.somehow.work.prompthub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somehow.work.prompthub.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 扣减余额（行级锁安全），返回影响行数 */
    @Update("UPDATE user SET balance = balance - #{amount} WHERE id = #{userId} AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
