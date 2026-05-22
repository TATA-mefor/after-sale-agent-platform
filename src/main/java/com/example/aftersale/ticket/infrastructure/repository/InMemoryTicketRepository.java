package com.example.aftersale.ticket.infrastructure.repository;

import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.ticket.domain.TicketRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
/*
默认本地测试/运行用内存实现
只要没有启用 mysql profile，就用这个。它内部就是一个 ConcurrentHashMap。
如果启用 mysql profile，就用 JdbcTicketRepository.java (line 19)
TicketRepository 只负责保存和读取工单。
默认用 InMemoryTicketRepository 存在内存里；启用 mysql profile 时用 JdbcTicketRepository 写 MySQL。
Repository 不决定业务流程，也不判断能不能创建、能不能关闭。
*/
@Repository
@Profile("!mysql")
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    @Override
    public Ticket save(Ticket ticket) {
        tickets.put(ticket.getTicketId(), ticket);
        return ticket;
    }

    @Override
    public Optional<Ticket> findById(String ticketId) {
        return Optional.ofNullable(tickets.get(ticketId));
    }
}
