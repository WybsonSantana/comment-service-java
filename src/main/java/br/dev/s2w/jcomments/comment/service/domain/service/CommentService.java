package br.dev.s2w.jcomments.comment.service.domain.service;

import br.dev.s2w.jcomments.comment.service.api.client.ModerationClient;
import br.dev.s2w.jcomments.comment.service.api.model.CommentInput;
import br.dev.s2w.jcomments.comment.service.api.model.ModerationRequest;
import br.dev.s2w.jcomments.comment.service.api.model.ModerationResponse;
import br.dev.s2w.jcomments.comment.service.domain.exception.CommentNotFoundException;
import br.dev.s2w.jcomments.comment.service.domain.exception.ModerationRejectedException;
import br.dev.s2w.jcomments.comment.service.domain.model.Comment;
import br.dev.s2w.jcomments.comment.service.domain.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ModerationClient moderationClient;

    public Page<Comment> getAllComments(Pageable pageable) {
        log.info("Fetching comments");
        Page<Comment> comments = commentRepository.findAll(pageable);

        log.info("Comments fetched successfully. Total elements: {}", comments.getTotalElements());
        return comments;
    }

    public Comment getCommentById(UUID id) {
        log.info("Fetching comment with ID: {}", id);
        Comment comment = commentRepository.findById(id)
                .orElseThrow(CommentNotFoundException::new);

        log.info("Comment found successfully. ID: {}", comment.getId());
        return comment;
    }

    public Comment createComment(CommentInput input) {
        log.info("Starting comment creation");
        Comment comment = Comment.builder()
                .text(input.getText())
                .author(input.getAuthor())
                .build();


        log.info("Sending comment for moderation. ID: {}", comment.getId());
        ModerationRequest request = new ModerationRequest(comment.getId(), input.getText());
        ModerationResponse response = moderationClient.moderateComment(request);

        if (!response.isApproved()) {
            log.info("Comment moderation rejected. ID: {}, Reason: {}", comment.getId(), response.getReason());
            throw new ModerationRejectedException(response.getReason());
        }

        log.info("Comment moderation approved. Saving comment. ID: {}", comment.getId());
        Comment savedComment = commentRepository.saveAndFlush(comment);

        log.info("Comment created successfully. ID: {}", savedComment.getId());
        return savedComment;
    }

}
