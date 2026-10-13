package br.dev.s2w.jcomments.comment.service.api.client;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class ModerationClientBadGatewayException extends RuntimeException {
}
