package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.aftersale.agent.domain.AgentRunRepository;
import com.example.aftersale.agent.infrastructure.repository.InMemoryAgentRunRepository;
import com.example.aftersale.approval.domain.ApprovalRepository;
import com.example.aftersale.approval.infrastructure.repository.InMemoryApprovalRepository;
import com.example.aftersale.order.domain.OrderRepository;
import com.example.aftersale.order.infrastructure.repository.InMemoryOrderRepository;
import com.example.aftersale.policy.domain.PolicyRepository;
import com.example.aftersale.policy.infrastructure.repository.InMemoryPolicyRepository;
import com.example.aftersale.ticket.domain.TicketRepository;
import com.example.aftersale.ticket.infrastructure.repository.InMemoryTicketRepository;
import com.example.aftersale.trace.domain.ToolCallTraceRepository;
import com.example.aftersale.trace.infrastructure.repository.InMemoryToolCallTraceRepository;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class PersistenceProfileTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private AgentRunRepository agentRunRepository;

    @Autowired
    private ToolCallTraceRepository toolCallTraceRepository;

    @Autowired
    private ApprovalRepository approvalRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Test
    void defaultProfileUsesInMemoryRepositoriesAndNoDataSource() {
        assertThat(ticketRepository).isInstanceOf(InMemoryTicketRepository.class);
        assertThat(agentRunRepository).isInstanceOf(InMemoryAgentRunRepository.class);
        assertThat(toolCallTraceRepository).isInstanceOf(InMemoryToolCallTraceRepository.class);
        assertThat(approvalRepository).isInstanceOf(InMemoryApprovalRepository.class);
        assertThat(orderRepository).isInstanceOf(InMemoryOrderRepository.class);
        assertThat(policyRepository).isInstanceOf(InMemoryPolicyRepository.class);
        assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();
    }

    @Test
    void mysqlRepositoriesAreNotActiveByDefault() {
        assertThat(applicationContext.getBeanNamesForType(DataSource.class)).isEmpty();
        assertThat(applicationContext).isNotNull();
        assertThatThrownByDataSourceLookup();
    }

    private void assertThatThrownByDataSourceLookup() {
        try {
            applicationContext.getBean(DataSource.class);
        } catch (NoSuchBeanDefinitionException exception) {
            assertThat(exception).hasMessageContaining("javax.sql.DataSource");
        }
    }
}
