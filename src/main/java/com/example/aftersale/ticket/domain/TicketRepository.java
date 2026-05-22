package com.example.aftersale.ticket.domain;

import java.util.Optional;
/*
 * 工单仓储接口
Repository 只是保存，不决定业务规则
它只有两个能力：保存、按 ID 查询。
 */
public interface TicketRepository {

    Ticket save(Ticket ticket);

    Optional<Ticket> findById(String ticketId);
}
