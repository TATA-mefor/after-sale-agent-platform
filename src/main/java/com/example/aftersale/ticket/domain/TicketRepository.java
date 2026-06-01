package com.example.aftersale.ticket.domain;

import java.util.Optional;
/*
 * 工单仓储接口
Repository 只是保存，不决定业务规则
它只提供持久化读写能力，不决定业务状态流转。
 */
public interface TicketRepository {

    Ticket save(Ticket ticket);

    Optional<Ticket> findById(String ticketId);

    TicketPage findPage(TicketQueryCriteria criteria);
}
