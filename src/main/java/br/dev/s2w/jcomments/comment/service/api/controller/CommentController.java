package br.dev.s2w.jcomments.comment.service.api.controller;

import br.dev.s2w.jcomments.comment.service.api.model.CommentInput;
import br.dev.s2w.jcomments.comment.service.api.model.CommentOutput;
import br.dev.s2w.jcomments.comment.service.domain.model.Comment;
import br.dev.s2w.jcomments.comment.service.domain.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public Page<CommentOutput> getAllComments(Pageable pageable) {
        Page<Comment> comments = commentService.getAllComments(pageable);

        return comments.map(this::convertToModel);
    }

    @GetMapping("/{id}")
    public CommentOutput getCommentById(@PathVariable String id) {
        Comment comment = commentService.getCommentById(UUID.fromString(id));

        return convertToModel(comment);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentOutput createComment(@RequestBody CommentInput input) {
        Comment comment = commentService.createComment(input);

        return convertToModel(comment);
    }

    private CommentOutput convertToModel(Comment comment) {
        return CommentOutput.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(comment.getAuthor())
                .createdAt(comment.getCreatedAt())
                .build();
    }


}