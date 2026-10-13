package br.dev.s2w.jcomments.comment.service.api.controller;

import br.dev.s2w.jcomments.comment.service.api.client.ModerationClient;
import br.dev.s2w.jcomments.comment.service.api.client.ModerationClientBadGatewayException;
import br.dev.s2w.jcomments.comment.service.api.model.ModerationResponse;
import br.dev.s2w.jcomments.comment.service.domain.model.Comment;
import br.dev.s2w.jcomments.comment.service.domain.repository.CommentRepository;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(
        scripts = "/data/cleanup.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS
)
class CommentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CommentRepository commentRepository;

    @MockitoBean
    ModerationClient moderationClient;

    @Test
    @DisplayName("Should return paginated comments")
    @Sql(
            scripts = {"/data/cleanup.sql", "/data/seed-comments.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    void shouldReturnPaginatedComments() throws Exception {
        mockMvc.perform(get("/api/comments")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value("a7f3c2e9-6b41-4d85-9a20-3e8f1b5c7d64"))
                .andExpect(jsonPath("$.content[0].text").value("Java é muito top, mas prefiro Kotlin."))
                .andExpect(jsonPath("$.content[0].author").value("Wybson Santana"))
                .andExpect(jsonPath("$.content[1].id").value("c9e2b6a4-1d73-48f0-b5a9-7c3e2d8f6140"))
                .andExpect(jsonPath("$.content[1].text").value("Java é muito bom, mas ainda prefiro Kotlin."))
                .andExpect(jsonPath("$.content[1].author").value("Wybson Santana"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("Should return a comment by ID")
    @Sql(
            scripts = {"/data/cleanup.sql", "/data/seed-comments.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    void shouldReturnCommentById() throws Exception {
        mockMvc.perform(get("/api/comments/{id}", UUID.fromString("a7f3c2e9-6b41-4d85-9a20-3e8f1b5c7d64")))
                .andExpect(status().isOk())
                .andExpect((ResultMatcher) jsonPath("$.id").value("a7f3c2e9-6b41-4d85-9a20-3e8f1b5c7d64"))
                .andExpect(jsonPath("$.text").value("Java é muito top, mas prefiro Kotlin."))
                .andExpect(jsonPath("$.author").value("Wybson Santana"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-12T10:00:00Z"));
    }

    @Test
    @DisplayName("should create a comment when moderation is approved")
    @Sql(
            scripts = {"/data/cleanup.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    void shouldCreateCommentWhenModerationIsApproved() throws Exception {
        when(moderationClient.moderateComment(any()))
                .thenReturn(ModerationResponse.builder()
                        .approved(true)
                        .reason("Comentário aprovado: nenhuma palavra proibida encontrada.")
                        .build());

        String response = mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "text": "Java é muito top, mas prefiro Kotlin.",
                                            "author": "Wybson Santana"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", Matchers.matchesPattern(
                        "^[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")))
                .andExpect(jsonPath("$.text", Matchers.equalTo("Java é muito top, mas prefiro Kotlin.")))
                .andExpect(jsonPath("$.author", Matchers.equalTo("Wybson Santana")))
                .andExpect(jsonPath("$.createdAt", Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID commentId = UUID.fromString(JsonPath.read(response, "$.id"));

        String createdAt = JsonPath.read(response, "$.createdAt");
        OffsetDateTime createdAtDateTime = OffsetDateTime.parse(createdAt);

        Comment savedComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AssertionError("Comment was not persisted"));

        assertEquals("Java é muito top, mas prefiro Kotlin.", savedComment.getText());
        assertEquals("Wybson Santana", savedComment.getAuthor());
        assertNotNull(savedComment.getCreatedAt());
        assertNotNull(createdAtDateTime);
    }

    @Test
    @DisplayName("should reject a comment with a banned word")
    void shouldRejectCommentWhenModerationDetectsABannedWord() throws Exception {
        when(moderationClient.moderateComment(any()))
                .thenReturn(ModerationResponse.builder()
                        .approved(false)
                        .reason("Contém palavras proibidas: foda")
                        .build());

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "text": "Java é muito top, mas prefiro Kotlin; foda demais!",
                                            "author": "Wybson Santana"
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", Matchers.equalTo("/errors/unprocessable-entity")))
                .andExpect(jsonPath("$.title", Matchers.equalTo("Unprocessable Entity")))
                .andExpect(jsonPath("$.status", Matchers.equalTo(422)))
                .andExpect(jsonPath("$.detail", Matchers.equalTo("Contém palavras proibidas: foda")))
                .andExpect(jsonPath("$.instance", Matchers.equalTo("/api/comments")));
    }

    @Test
    @DisplayName("should reject a comment with banned words")
    void shouldRejectCommentWhenModerationDetectsBannedWords() throws Exception {
        when(moderationClient.moderateComment(any()))
                .thenReturn(ModerationResponse.builder()
                        .approved(false)
                        .reason("Contém palavras proibidas: foda, puta")
                        .build());

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "text": "Java é muito top, mas prefiro Kotlin; foda demais, puta que pariu!",
                                            "author": "Wybson Santana"
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", Matchers.equalTo("/errors/unprocessable-entity")))
                .andExpect(jsonPath("$.title", Matchers.equalTo("Unprocessable Entity")))
                .andExpect(jsonPath("$.status", Matchers.equalTo(422)))
                .andExpect(jsonPath("$.detail", Matchers.equalTo("Contém palavras proibidas: foda, puta")))
                .andExpect(jsonPath("$.instance", Matchers.equalTo("/api/comments")));
    }

    @Test
    @DisplayName("should return 504 when moderation service times out")
    void shouldReturn504WhenModerationServiceTimesOut() throws Exception {
        when(moderationClient.moderateComment(any()))
                .thenThrow(new org.springframework.web.client.ResourceAccessException(
                        "Connection refused: getsockopt",
                        new java.net.SocketTimeoutException("Connection refused: getsockopt")
                ));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "text": "Java é muito top, mas prefiro Kotlin.",
                                            "author": "Wybson Santana"
                                        }
                                        """)
                )
                .andExpect(status().isGatewayTimeout())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", Matchers.equalTo("/errors/gateway-timeout")))
                .andExpect(jsonPath("$.title", Matchers.equalTo("Gateway timeout")))
                .andExpect(jsonPath("$.status", Matchers.equalTo(504)))
                .andExpect(jsonPath("$.detail", Matchers.equalTo("Connection refused: getsockopt")))
                .andExpect(jsonPath("$.instance", Matchers.equalTo("/api/comments")));
    }

    @Test
    @DisplayName("should return 502 when moderation service fails unexpectedly")
    void shouldReturn502WhenModerationServiceFailsUnexpectedly() throws Exception {
        when(moderationClient.moderateComment(any()))
                .thenThrow(new ModerationClientBadGatewayException());

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "text": "Java é muito top, mas prefiro Kotlin.",
                                            "author": "Wybson Santana"
                                        }
                                        """)
                )
                .andExpect(status().isBadGateway())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("/errors/bad-gateway"))
                .andExpect(jsonPath("$.title", Matchers.equalTo("Bad Gateway")))
                .andExpect(jsonPath("$.status", Matchers.equalTo(502)))
                .andExpect(jsonPath("$.detail", Matchers.equalTo(
                        "An unexpected error occurred while communicating with the moderation service."
                )))
                .andExpect(jsonPath("$.instance", Matchers.equalTo("/api/comments")));
    }

    @Test
    @DisplayName("should return 404 when comment ID does not exist")
    void shouldReturnNotFoundWhenCommentIdDoesNotExist() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                "/api/comments/{id}",
                                UUID.fromString("8f2a6c91-7b43-4d5e-a019-6c8e2f5b739d")
                        )
                )
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

}
