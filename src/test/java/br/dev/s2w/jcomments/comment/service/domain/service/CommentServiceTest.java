package br.dev.s2w.jcomments.comment.service.domain.service;

import br.dev.s2w.jcomments.comment.service.api.client.ModerationClient;
import br.dev.s2w.jcomments.comment.service.api.model.CommentInput;
import br.dev.s2w.jcomments.comment.service.api.model.ModerationRequest;
import br.dev.s2w.jcomments.comment.service.api.model.ModerationResponse;
import br.dev.s2w.jcomments.comment.service.domain.exception.CommentNotFoundException;
import br.dev.s2w.jcomments.comment.service.domain.exception.ModerationRejectedException;
import br.dev.s2w.jcomments.comment.service.domain.model.Comment;
import br.dev.s2w.jcomments.comment.service.domain.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final UUID COMMENT_ID =
            UUID.fromString("a7f3c2e9-6b41-4d85-9a20-3e8f1b5c7d64");

    private static final UUID NON_EXISTENT_COMMENT_ID =
            UUID.fromString("8f2a6c91-7b43-4d5e-a019-6c8e2f5b739d");

    private static final String DEFAULT_COMMENT_TEXT =
            "Java é muito top, mas prefiro Kotlin.";

    private static final String DEFAULT_AUTHOR =
            "Wybson Santana";

    private static final String REJECTED_COMMENT_TEXT =
            "Java é muito top, mas prefiro Kotlin; foda demais!";

    private static final String REJECTION_REASON =
            "Contém palavras proibidas: foda";

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ModerationClient moderationClient;

    @InjectMocks
    private CommentService commentService;

    @Test
    void shouldReturnPaginatedComments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Comment> expectedPage = new PageImpl<>(List.of(), pageable, 0);

        when(commentRepository.findAll(pageable))
                .thenReturn(expectedPage);

        Page<Comment> actualPage = commentService.getAllComments(pageable);

        assertSame(expectedPage, actualPage);
        verify(commentRepository).findAll(pageable);
    }

    @Test
    void shouldReturnCommentWhenIdExists() {
        Comment expectedComment = createComment();

        when(commentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(expectedComment));

        Comment actualComment = commentService.getCommentById(COMMENT_ID);

        assertSame(expectedComment, actualComment);
        verify(commentRepository).findById(COMMENT_ID);
    }

    @Test
    void shouldCreateCommentWhenModerationApprovesIt() {
        CommentInput input = createCommentInput();

        when(moderationClient.moderateComment(any(ModerationRequest.class)))
                .thenReturn(createModerationResponse(true, null));

        when(commentRepository.saveAndFlush(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Comment actualComment = commentService.createComment(input);

        assertEquals(input.getText(), actualComment.getText());
        assertEquals(input.getAuthor(), actualComment.getAuthor());

        verify(moderationClient).moderateComment(any(ModerationRequest.class));
        verify(commentRepository).saveAndFlush(any(Comment.class));
    }

    @Test
    void shouldThrowExceptionWhenCommentIdDoesNotExist() {
        when(commentRepository.findById(NON_EXISTENT_COMMENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                CommentNotFoundException.class,
                () -> commentService.getCommentById(NON_EXISTENT_COMMENT_ID)
        );

        verify(commentRepository).findById(NON_EXISTENT_COMMENT_ID);
    }

    @Test
    void shouldRejectCommentWhenModerationRejectsIt() {
        CommentInput input = createCommentInput(REJECTED_COMMENT_TEXT);

        when(moderationClient.moderateComment(any(ModerationRequest.class)))
                .thenReturn(createModerationResponse(false, REJECTION_REASON));

        assertThrows(
                ModerationRejectedException.class,
                () -> commentService.createComment(input)
        );

        verify(moderationClient).moderateComment(any(ModerationRequest.class));
        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    private Comment createComment() {
        return Comment.builder()
                .id(COMMENT_ID)
                .text(DEFAULT_COMMENT_TEXT)
                .author(DEFAULT_AUTHOR)
                .build();
    }

    private CommentInput createCommentInput() {
        return createCommentInput(DEFAULT_COMMENT_TEXT);
    }

    private CommentInput createCommentInput(String text) {
        return CommentInput.builder()
                .text(text)
                .author(DEFAULT_AUTHOR)
                .build();
    }

    private ModerationResponse createModerationResponse(
            boolean approved,
            String reason
    ) {
        return ModerationResponse.builder()
                .approved(approved)
                .reason(reason)
                .build();
    }

}
