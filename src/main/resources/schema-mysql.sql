CREATE TABLE IF NOT EXISTS tickets (
    ticket_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    raw_user_message TEXT NOT NULL,
    intent_type VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    status VARCHAR(64) NOT NULL,
    internal_note TEXT NULL,
    agent_suggestion TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX idx_tickets_user_id (user_id),
    INDEX idx_tickets_order_id (order_id),
    INDEX idx_tickets_status (status)
);

CREATE TABLE IF NOT EXISTS agent_runs (
    run_id VARCHAR(64) PRIMARY KEY,
    ticket_id VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    plan_json LONGTEXT NULL,
    final_answer LONGTEXT NULL,
    error_message TEXT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    finished_at TIMESTAMP(6) NULL,
    INDEX idx_agent_runs_ticket_id (ticket_id),
    INDEX idx_agent_runs_status (status),
    CONSTRAINT fk_agent_runs_ticket_id
        FOREIGN KEY (ticket_id) REFERENCES tickets (ticket_id)
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS tool_call_traces (
    trace_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    input_json LONGTEXT NOT NULL,
    status VARCHAR(64) NOT NULL,
    output_json LONGTEXT NULL,
    latency_ms BIGINT NOT NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_tool_call_traces_run_id (run_id),
    INDEX idx_tool_call_traces_tool_name (tool_name),
    INDEX idx_tool_call_traces_status (status),
    CONSTRAINT fk_tool_call_traces_run_id
        FOREIGN KEY (run_id) REFERENCES agent_runs (run_id)
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS approval_requests (
    approval_id VARCHAR(64) PRIMARY KEY,
    ticket_id VARCHAR(64) NOT NULL,
    run_id VARCHAR(64) NOT NULL,
    subtask_id VARCHAR(64) NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    requested_action TEXT NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    status VARCHAR(64) NOT NULL,
    reviewer_id VARCHAR(64) NULL,
    decision_reason TEXT NULL,
    requested_at TIMESTAMP(6) NOT NULL,
    reviewed_at TIMESTAMP(6) NULL,
    INDEX idx_approval_requests_ticket_id (ticket_id),
    INDEX idx_approval_requests_run_id (run_id),
    INDEX idx_approval_requests_status (status),
    CONSTRAINT fk_approval_requests_ticket_id
        FOREIGN KEY (ticket_id) REFERENCES tickets (ticket_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_approval_requests_run_id
        FOREIGN KEY (run_id) REFERENCES agent_runs (run_id)
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    order_status VARCHAR(64) NOT NULL,
    paid_amount DECIMAL(18, 2) NOT NULL,
    paid_at TIMESTAMP(6) NOT NULL,
    delivered_at TIMESTAMP(6) NULL,
    aftersale_deadline TIMESTAMP(6) NOT NULL,
    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_order_status (order_status)
);

CREATE TABLE IF NOT EXISTS products (
    product_id VARCHAR(64) PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    department VARCHAR(255) NULL,
    division VARCHAR(255) NULL,
    source_dataset VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_products_category (category),
    INDEX idx_products_source_dataset (source_dataset)
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(18, 2) NOT NULL,
    category VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_order_items_order_id (order_id),
    INDEX idx_order_items_product_id (product_id),
    CONSTRAINT fk_order_items_order_id
        FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_order_items_product_id
        FOREIGN KEY (product_id) REFERENCES products (product_id)
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS aftersale_policies (
    policy_id VARCHAR(64) PRIMARY KEY,
    category VARCHAR(255) NOT NULL,
    product_type VARCHAR(255) NOT NULL,
    policy_text TEXT NOT NULL,
    effective_from TIMESTAMP(6) NOT NULL,
    effective_to TIMESTAMP(6) NOT NULL,
    INDEX idx_aftersale_policies_category (category),
    INDEX idx_aftersale_policies_product_type (product_type)
);
