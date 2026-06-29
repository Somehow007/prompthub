package com.somehow.work.prompthub;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.somehow.work.prompthub.dto.CreateActivityDTO;
import com.somehow.work.prompthub.entity.ActivityTemplate;
import com.somehow.work.prompthub.entity.Order;
import com.somehow.work.prompthub.entity.Template;
import com.somehow.work.prompthub.entity.User;
import com.somehow.work.prompthub.mapper.*;
import com.somehow.work.prompthub.service.ActivityService;
import com.somehow.work.prompthub.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并发测试：验证分布式锁和事务在并发场景下的数据一致性
 *
 * 测试场景：
 * 1. 活动限量 5 份，20 个不同用户并发领取 → 恰好 5 人成功
 * 2. 并发购买同一模板 → 只有一个成功
 */
@SpringBootTest
@Disabled("需要运行中的 MySQL + Redis 环境，手动运行：mvn test -Dtest=ConcurrencyTest -Dspring.datasource.password=<pwd>")
class ConcurrencyTest {

    @Autowired private ActivityService activityService;
    @Autowired private ActivityTemplateMapper activityTemplateMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private TemplateMapper templateMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private OrderService orderService;

    private static final int TOTAL_QUOTA = 5;
    private static final int CONCURRENT_USERS = 20;
    private static final String TEST_PREFIX = "concur_test_";

    private Long testTemplateId;
    private Long testActivityId;
    private List<Long> testUserIds;

    @BeforeEach
    void setUp() {
        // 1. 创建测试模板（付费）
        Template template = new Template();
        template.setCreatorId(1L); // admin
        template.setTitle(TEST_PREFIX + "template_" + System.currentTimeMillis());
        template.setDescription("并发测试模板");
        template.setPrice(BigDecimal.valueOf(9.90));
        template.setStatus(1);
        template.setCurrentVersion(1);
        templateMapper.insert(template);
        testTemplateId = template.getId();

        // 2. 创建活动：限量 5 份
        CreateActivityDTO dto = new CreateActivityDTO();
        dto.setTemplateId(testTemplateId);
        dto.setStartTime(LocalDateTime.now().minusMinutes(5));
        dto.setEndTime(LocalDateTime.now().plusMinutes(30));
        dto.setTotalQuota(TOTAL_QUOTA);
        var activity = activityService.createActivity(dto);
        testActivityId = activity.getId();

        // 3. 创建测试用户（各不相同）
        long timestamp = System.currentTimeMillis();
        testUserIds = new ArrayList<>();
        for (int i = 1; i <= CONCURRENT_USERS; i++) {
            User user = new User();
            user.setUsername(TEST_PREFIX + "u" + timestamp + "_" + i);
            user.setPasswordHash("$2a$10$test");
            user.setBalance(BigDecimal.valueOf(100));
            user.setRole("user");
            user.setStatus(1);
            userMapper.insert(user);
            testUserIds.add(user.getId());
        }
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        if (testActivityId != null) {
            activityTemplateMapper.deleteById(testActivityId);
        }
        if (testTemplateId != null) {
            templateMapper.deleteById(testTemplateId);
        }
        if (testUserIds != null) {
            for (Long uid : testUserIds) {
                // 清理订单
                List<Order> orders = orderMapper.selectList(
                        new LambdaQueryWrapper<Order>()
                                .eq(Order::getUserId, uid));
                orders.forEach(o -> orderMapper.deleteById(o.getId()));
                userMapper.deleteById(uid);
            }
        }
    }

    /**
     * 并发领取测试：20人同时抢5份限量活动
     * 预期：恰好 5 人成功，剩余份数 = 0
     */
    @Test
    @DisplayName("并发领取活动 — 20人抢5份，恰好5人成功")
    void testConcurrentClaim() throws InterruptedException {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_USERS);

        // 启动 CONCURRENT_USERS 个线程
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // 等待发令枪
                    activityService.claim(testActivityId, testUserIds.get(idx));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }, "claim-thread-" + i).start();
        }

        // 发令枪：所有线程同时开始
        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();

        // 验证
        assertEquals(TOTAL_QUOTA, successCount.get(),
                "恰好 " + TOTAL_QUOTA + " 人成功");
        assertEquals(CONCURRENT_USERS - TOTAL_QUOTA, failCount.get(),
                "其余 " + (CONCURRENT_USERS - TOTAL_QUOTA) + " 人失败");

        // 验证数据库库存
        ActivityTemplate activity = activityTemplateMapper.selectById(testActivityId);
        assertNotNull(activity);
        assertEquals(0, activity.getRemainingQuota(), "剩余份数应为 0");
    }

    /**
     * 并发购买测试：同一用户同时多次购买同一模板
     * 预期：只有一次成功
     */
    @Test
    @DisplayName("并发购买模板 — 同一用户只购买成功一次")
    void testConcurrentPurchase() throws InterruptedException {
        // 使用第一个测试用户
        Long userId = testUserIds.get(0);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        int attempts = 5;
        CountDownLatch readyLatch = new CountDownLatch(attempts);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(attempts);

        for (int i = 0; i < attempts; i++) {
            new Thread(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    orderService.purchase(testTemplateId, userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }, "purchase-thread-" + i).start();
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();

        // 验证：只有一次成功
        assertEquals(1, successCount.get(), "只有一次购买成功");
        assertEquals(attempts - 1, failCount.get(), "其余购买失败");

        // 验证数据库中只有一个订单
        Long orderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(Order::getTemplateId, testTemplateId)
                        .eq(Order::getStatus, 1));
        assertEquals(1L, orderCount.longValue(), "数据库只有一个已完成订单");
    }
}
