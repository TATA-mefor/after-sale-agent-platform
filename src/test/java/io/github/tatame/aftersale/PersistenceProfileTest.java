package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tatame.aftersale.agent.domain.AgentRunRepository;
import io.github.tatame.aftersale.agent.infrastructure.repository.InMemoryAgentRunRepository;
import io.github.tatame.aftersale.approval.domain.ApprovalRepository;
import io.github.tatame.aftersale.approval.infrastructure.repository.InMemoryApprovalRepository;
import io.github.tatame.aftersale.order.domain.OrderRepository;
import io.github.tatame.aftersale.order.infrastructure.repository.InMemoryOrderRepository;
import io.github.tatame.aftersale.policy.domain.PolicyRepository;
import io.github.tatame.aftersale.policy.infrastructure.repository.InMemoryPolicyRepository;
import io.github.tatame.aftersale.policy.rag.infrastructure.pgvector.PgVectorProfileGuard;
import io.github.tatame.aftersale.ticket.domain.TicketRepository;
import io.github.tatame.aftersale.ticket.infrastructure.repository.InMemoryTicketRepository;
import io.github.tatame.aftersale.trace.domain.ToolCallTraceRepository;
import io.github.tatame.aftersale.trace.infrastructure.repository.InMemoryToolCallTraceRepository;
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
        assertThat(applicationContext.getBeansOfType(PgVectorProfileGuard.class)).isEmpty();
    }

    @Test
    void mysqlRepositoriesAreNotActiveByDefault() {
        assertThat(applicationContext.getBeanNamesForType(DataSource.class)).isEmpty();
        assertThat(applicationContext).isNotNull();
        assertThatThrownByDataSourceLookup();
    }

    @Test
    void pgVectorInfrastructureIsNotActiveByDefault() {
        assertThat(applicationContext.getBeansOfType(PgVectorProfileGuard.class)).isEmpty();
        assertThat(pgVectorRuntimeBeanNames()).isEmpty();
    }

    private void assertThatThrownByDataSourceLookup() {
        try {
            applicationContext.getBean(DataSource.class);
        } catch (NoSuchBeanDefinitionException exception) {
            assertThat(exception).hasMessageContaining("javax.sql.DataSource");
        }
    }

    private String[] pgVectorRuntimeBeanNames() {
        return java.util.Arrays.stream(applicationContext.getBeanDefinitionNames())
                .filter(beanName -> beanName.contains("VectorStore"))
                .filter(beanName -> !beanName.equals("ragVectorStoreHealthIndicator"))
                .toArray(String[]::new);
    }
}
