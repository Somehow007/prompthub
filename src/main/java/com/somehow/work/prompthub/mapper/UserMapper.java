package com.somehow.work.prompthub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somehow.work.prompthub.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
