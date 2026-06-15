package io.github.tatame.aftersale.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.tatame.aftersale.agent.infrastructure.springai.SpringAiChatGateway;
import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.infrastructure.pgvector.JdbcPolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.infrastructure.springai.SpringAiEmbeddingGateway;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityDefaultBoundaryTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityProperties securityProperties;

    @Test
    void defaultProfileKeepsExistingApiSurfacePermitAll() throws Exception {
        assertThat(securityProperties.enabled()).isFalse();

        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/api/tickets")).andExpect(status().isOk());
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "U-SEC-DEFAULT-1",
                                  "orderId": "O-SEC-DEFAULT-1",
                                  "message": "Default profile must not require an API key."
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void defaultProfileDoesNotCreateLiveDependencyBeans() {
        assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(PolicyVectorRepository.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(JdbcPolicyVectorRepository.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(SpringAiChatGateway.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(SpringAiEmbeddingGateway.class)).isEmpty();
        assertThat(beanNamesForOptionalType("org.springframework.ai.chat.model.ChatModel")).isEmpty();
        assertThat(beanNamesForOptionalType("org.springframework.ai.embedding.EmbeddingModel")).isEmpty();
        assertThat(beanNamesForOptionalType("org.springframework.ai.vectorstore.VectorStore")).isEmpty();
    }

    private String[] beanNamesForOptionalType(String className) {
        try {
            return applicationContext.getBeanNamesForType(Class.forName(className));
        } catch (ClassNotFoundException exception) {
            return new String[0];
        }
    }
}
