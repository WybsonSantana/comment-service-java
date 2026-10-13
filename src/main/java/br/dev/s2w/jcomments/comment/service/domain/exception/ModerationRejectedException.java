package br.dev.s2w.jcomments.comment.service.domain.exception;

public class ModerationRejectedException extends RuntimeException {

    public ModerationRejectedException(String message) {
        super(message);
    }

}
