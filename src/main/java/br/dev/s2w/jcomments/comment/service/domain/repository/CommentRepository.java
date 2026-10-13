package br.dev.s2w.jcomments.comment.service.domain.repository;

import br.dev.s2w.jcomments.comment.service.domain.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
}
