package com.medshop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 线上购药系统 启动类。
 *
 * <p>本工程实现三个功能模块：
 * <ul>
 *   <li>M1 基础购药（传统模块）：药品搜索/详情、购物车、下单、订单查询</li>
 *   <li>P3 用户与权限（传统模块）：注册/登录、收货地址、隐私授权</li>
 *   <li>M2 智能药师咨询（AI 模块）：经 AI 中台 P1 生成用药建议，低置信/高风险走 P2 转人工</li>
 * </ul>
 */
@SpringBootApplication
@MapperScan("com.medshop.**.mapper")
@ConfigurationPropertiesScan("com.medshop")
public class MedshopApplication {

    public static void main(String[] args) {
        // SQLite 不会自动创建数据库文件所在目录，启动前确保 ./data 存在
        new java.io.File("data").mkdirs();
        SpringApplication.run(MedshopApplication.class, args);
    }
}
