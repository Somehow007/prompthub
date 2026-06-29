package com.somehow.work.prompthub.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.somehow.work.prompthub.dto.LoginDTO;
import com.somehow.work.prompthub.dto.LoginResultDTO;
import com.somehow.work.prompthub.dto.RegisterDTO;
import com.somehow.work.prompthub.entity.*;
import com.somehow.work.prompthub.exception.BusinessException;
import com.somehow.work.prompthub.mapper.*;
import com.somehow.work.prompthub.service.OrderService;
import com.somehow.work.prompthub.service.UserService;
import com.somehow.work.prompthub.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final TemplateMapper templateMapper;
    private final FavoriteMapper favoriteMapper;
    private final IncomeRecordMapper incomeRecordMapper;
    private final TemplateTagMapper templateTagMapper;
    private final TagMapper tagMapper;
    private final OrderService orderService;

    @Override
    public UserVO register(RegisterDTO dto) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已被注册");
        }

        // 检查邮箱是否已存在
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
            if (count > 0) {
                throw new BusinessException("邮箱已被注册");
            }
        }

        // 构建用户实体
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setCreatorLevel(0);
        user.setBalance(java.math.BigDecimal.ZERO);
        user.setRole("user");
        user.setStatus(1);

        userMapper.insert(user);
        log.info("用户注册成功: {}", user.getUsername());

        return toVO(user);
    }

    @Override
    public LoginResultDTO login(LoginDTO dto) {
        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 验证密码
        if (!BCrypt.checkpw(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        // Sa-Token 登录
        StpUtil.login(user.getId());
        // 存储角色信息到 Session，供 AdminActivityController 等鉴权使用
        StpUtil.getSession().set("role", user.getRole());

        // 构建返回
        String token = StpUtil.getTokenValue();
        log.info("用户登录成功: {}", user.getUsername());

        return LoginResultDTO.builder()
                .token(token)
                .user(toVO(user))
                .build();
    }

    @Override
    public UserVO currentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(user);
    }

    @Override
    public UserVO recharge(Long userId, BigDecimal amount) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setBalance(user.getBalance().add(amount));
        userMapper.updateById(user);
        log.info("用户充值: userId={}, amount={}", userId, amount);
        return toVO(user);
    }

    @Override
    public UserProfileVO profile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 该用户发布的模板
        List<Template> templates = templateMapper.selectList(
                new LambdaQueryWrapper<Template>().eq(Template::getCreatorId, userId)
                        .orderByDesc(Template::getCreatedAt));

        long templateCount = templates.size();

        // 总使用次数
        long totalUseCount = templates.stream()
                .mapToLong(t -> t.getUseCount() != null ? t.getUseCount() : 0)
                .sum();

        // 收藏数
        Long favoriteCount = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId));

        // 总收入
        List<IncomeRecord> allIncomes = incomeRecordMapper.selectList(
                new LambdaQueryWrapper<IncomeRecord>().eq(IncomeRecord::getUserId, userId));
        BigDecimal totalIncome = allIncomes.stream()
                .map(IncomeRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 收入明细（带模板标题）
        final Map<Long, String> titleMap;
        if (!templates.isEmpty()) {
            titleMap = templates.stream()
                    .collect(Collectors.toMap(Template::getId, Template::getTitle));
        } else {
            titleMap = Map.of();
        }
        List<IncomeRecordVO> incomeVOs = allIncomes.stream()
                .map(i -> IncomeRecordVO.builder()
                        .id(i.getId())
                        .templateId(i.getTemplateId())
                        .templateTitle(titleMap.getOrDefault(i.getTemplateId(), ""))
                        .orderId(i.getOrderId())
                        .amount(i.getAmount())
                        .type(i.getType())
                        .createdAt(i.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // 最近模板（转 VO）
        List<TemplateVO> recentTemplates = List.of();
        if (!templates.isEmpty()) {
            List<Template> recent = templates.stream().limit(6).toList();

            // 批量查标签
            Set<Long> tIds = recent.stream().map(Template::getId).collect(Collectors.toSet());
            List<TemplateTag> allTTs = templateTagMapper.selectList(
                    new LambdaQueryWrapper<TemplateTag>().in(TemplateTag::getTemplateId, tIds));
            Map<Long, List<TemplateTag>> ttMap = allTTs.stream()
                    .collect(Collectors.groupingBy(TemplateTag::getTemplateId));
            Set<Long> tagIds = allTTs.stream().map(TemplateTag::getTagId).collect(Collectors.toSet());
            final Map<Long, Tag> tagMap;
            if (!tagIds.isEmpty()) {
                tagMap = tagMapper.selectBatchIds(tagIds).stream()
                        .collect(Collectors.toMap(Tag::getId, t -> t));
            } else {
                tagMap = Map.of();
            }

            recentTemplates = recent.stream()
                    .map(t -> {
                        List<TemplateTag> tts = ttMap.getOrDefault(t.getId(), List.of());
                        List<Long> tIdList = tts.stream().map(TemplateTag::getTagId).toList();
                        List<String> tNames = tts.stream()
                                .map(tt -> {
                                    Tag tag = tagMap.get(tt.getTagId());
                                    return tag != null ? tag.getName() : "";
                                })
                                .filter(n -> !n.isEmpty())
                                .toList();

                        return TemplateVO.builder()
                                .id(t.getId())
                                .creatorId(t.getCreatorId())
                                .creatorName(user.getUsername())
                                .creatorAvatar(user.getAvatarUrl())
                                .title(t.getTitle())
                                .description(t.getDescription())
                                .coverUrl(t.getCoverUrl())
                                .price(t.getPrice())
                                .status(t.getStatus())
                                .currentVersion(t.getCurrentVersion())
                                .useCount(t.getUseCount())
                                .favoriteCount(t.getFavoriteCount())
                                .reviewCount(t.getReviewCount())
                                .avgRating(t.getAvgRating())
                                .tagIds(tIdList)
                                .tagNames(tNames)
                                .createdAt(t.getCreatedAt())
                                .updatedAt(t.getUpdatedAt())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        // 已购模板（转 VO）
        List<TemplateVO> purchasedTemplates = List.of();
        List<Long> purchasedIds = orderService.getPurchasedTemplateIds(userId);
        if (!purchasedIds.isEmpty()) {
            List<Template> purchasedList = templateMapper.selectBatchIds(purchasedIds);
            // 过滤 null（可能模板已删除）
            purchasedList = purchasedList.stream().filter(t -> t != null).toList();

            if (!purchasedList.isEmpty()) {
                // 批量查创作者
                Set<Long> pCreatorIds = purchasedList.stream()
                        .map(Template::getCreatorId).collect(Collectors.toSet());
                Map<Long, User> creatorMap = Map.of();
                if (!pCreatorIds.isEmpty()) {
                    List<User> creators = userMapper.selectBatchIds(pCreatorIds);
                    creatorMap = creators.stream().collect(Collectors.toMap(User::getId, u -> u));
                }

                // 批量查标签
                Set<Long> pTIds = purchasedList.stream().map(Template::getId).collect(Collectors.toSet());
                List<TemplateTag> pAllTTs = templateTagMapper.selectList(
                        new LambdaQueryWrapper<TemplateTag>().in(TemplateTag::getTemplateId, pTIds));
                Map<Long, List<TemplateTag>> pTTMap = pAllTTs.stream()
                        .collect(Collectors.groupingBy(TemplateTag::getTemplateId));
                Set<Long> pTagIds = pAllTTs.stream().map(TemplateTag::getTagId).collect(Collectors.toSet());
                final Map<Long, Tag> pTagMap;
                if (!pTagIds.isEmpty()) {
                    pTagMap = tagMapper.selectBatchIds(pTagIds).stream()
                            .collect(Collectors.toMap(Tag::getId, t -> t));
                } else {
                    pTagMap = Map.of();
                }

                final Map<Long, User> finalCreatorMap = creatorMap;
                purchasedTemplates = purchasedList.stream()
                        .map(t -> {
                            User creator = finalCreatorMap.get(t.getCreatorId());
                            List<TemplateTag> tts = pTTMap.getOrDefault(t.getId(), List.of());
                            List<Long> tIdList = tts.stream().map(TemplateTag::getTagId).toList();
                            List<String> tNames = tts.stream()
                                    .map(tt -> {
                                        Tag tag = pTagMap.get(tt.getTagId());
                                        return tag != null ? tag.getName() : "";
                                    })
                                    .filter(n -> !n.isEmpty())
                                    .toList();

                            return TemplateVO.builder()
                                    .id(t.getId())
                                    .creatorId(t.getCreatorId())
                                    .creatorName(creator != null ? creator.getUsername() : "")
                                    .creatorAvatar(creator != null ? creator.getAvatarUrl() : null)
                                    .title(t.getTitle())
                                    .description(t.getDescription())
                                    .coverUrl(t.getCoverUrl())
                                    .price(t.getPrice())
                                    .status(t.getStatus())
                                    .currentVersion(t.getCurrentVersion())
                                    .useCount(t.getUseCount())
                                    .favoriteCount(t.getFavoriteCount())
                                    .reviewCount(t.getReviewCount())
                                    .avgRating(t.getAvgRating())
                                    .tagIds(tIdList)
                                    .tagNames(tNames)
                                    .createdAt(t.getCreatedAt())
                                    .updatedAt(t.getUpdatedAt())
                                    .build();
                        })
                        .collect(Collectors.toList());
            }
        }

        return UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .creatorLevel(user.getCreatorLevel())
                .balance(user.getBalance())
                .createdAt(user.getCreatedAt())
                .templateCount(templateCount)
                .favoriteCount(favoriteCount)
                .totalUseCount(totalUseCount)
                .totalIncome(totalIncome)
                .recentTemplates(recentTemplates)
                .incomeRecords(incomeVOs)
                .purchasedTemplates(purchasedTemplates)
                .build();
    }

    /** 实体转 VO */
    private UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .creatorLevel(user.getCreatorLevel())
                .balance(user.getBalance())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
