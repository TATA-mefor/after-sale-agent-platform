INSERT INTO orders (
    order_id, user_id, product_id, product_name, order_status,
    paid_amount, paid_at, delivered_at, aftersale_deadline
) VALUES
    (
        'O-PAID-NOT-SHIPPED', 'U-DEMO-ORDER', 'P-KEYBOARD-001', 'Mechanical Keyboard', 'PAID',
        299.00, '2026-05-12 10:00:00.000000', NULL, '2026-06-12 23:59:59.000000'
    ),
    (
        'O202605130001', 'U-DEMO-1', 'P-HEADPHONE-001', 'Wireless Headphones', 'DELIVERED',
        499.00, '2026-05-01 09:00:00.000000', '2026-05-10 15:00:00.000000',
        '2026-05-25 23:59:59.000000'
    ),
    (
        'O-7001', 'U-7001', 'P-HEADPHONE-001', 'Wireless Headphones', 'DELIVERED',
        499.00, '2026-05-01 09:00:00.000000', '2026-05-10 15:00:00.000000',
        '2026-05-25 23:59:59.000000'
    ),
    (
        'O-7002', 'U-7002', 'P-CHARGER-001', 'USB-C Charger', 'LOGISTICS_EXCEPTION',
        89.00, '2026-05-03 12:00:00.000000', NULL, '2026-06-03 23:59:59.000000'
    ),
    (
        'O-EXPIRED-AFTERSALE', 'U-DEMO-ORDER', 'P-SHOES-001', 'Running Shoes', 'DELIVERED',
        399.00, '2026-03-01 09:00:00.000000', '2026-03-05 14:00:00.000000',
        '2026-03-20 23:59:59.000000'
    ),
    (
        'O-SPECIAL-GOODS', 'U-DEMO-ORDER', 'P-CUSTOM-001', 'Customized Gift Box', 'DELIVERED',
        199.00, '2026-05-08 08:30:00.000000', '2026-05-12 18:00:00.000000',
        '2026-05-19 23:59:59.000000'
    )
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    product_id = VALUES(product_id),
    product_name = VALUES(product_name),
    order_status = VALUES(order_status),
    paid_amount = VALUES(paid_amount),
    paid_at = VALUES(paid_at),
    delivered_at = VALUES(delivered_at),
    aftersale_deadline = VALUES(aftersale_deadline);

INSERT INTO aftersale_policies (
    policy_id, category, product_type, policy_text, effective_from, effective_to
) VALUES
    (
        'POL-RETURN-7D', '7 天无理由退货规则', '通用商品',
        '用户签收商品后 7 天内，在商品完好、附件齐全且不影响二次销售时，可申请无理由退货。',
        '2026-01-01 00:00:00.000000', '2026-12-31 23:59:59.000000'
    ),
    (
        'POL-QUALITY-RETURN-EXCHANGE', '质量问题退换货规则', '通用商品',
        '商品存在质量问题、功能故障或与描述明显不符时，用户可申请退货、退款或换货。',
        '2026-01-01 00:00:00.000000', '2026-12-31 23:59:59.000000'
    ),
    (
        'POL-LOGISTICS-NOT-RECEIVED', '已签收未收到物流争议规则', '通用商品',
        '物流显示已签收但用户反馈未收到货时，应核验签收凭证、物流轨迹和收货地址后进入争议处理。',
        '2026-01-01 00:00:00.000000', '2026-12-31 23:59:59.000000'
    ),
    (
        'POL-EXCHANGE', '换货规则', '服饰鞋包',
        '尺码不合适、颜色错发或同款可替换库存充足时，可发起换货流程。',
        '2026-01-01 00:00:00.000000', '2026-12-31 23:59:59.000000'
    ),
    (
        'POL-REPAIR', '维修规则', '电子数码',
        '保修期内商品出现非人为损坏故障时，可申请维修；超过保修期需告知可能产生费用。',
        '2026-01-01 00:00:00.000000', '2026-12-31 23:59:59.000000'
    ),
    (
        'POL-SPECIAL-NO-RETURN', '特殊商品不支持退货规则', '特殊商品',
        '定制商品、生鲜易腐商品、拆封后影响安全或卫生的商品，非质量问题通常不支持退货。',
        '2026-01-01 00:00:00.000000', '2026-12-31 23:59:59.000000'
    )
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    product_type = VALUES(product_type),
    policy_text = VALUES(policy_text),
    effective_from = VALUES(effective_from),
    effective_to = VALUES(effective_to);
