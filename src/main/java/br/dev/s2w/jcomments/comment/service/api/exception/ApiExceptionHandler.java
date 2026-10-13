package br.dev.s2w.jcomments.comment.service.api.exception;

import br.dev.s2w.jcomments.comment.service.api.client.ModerationClientBadGatewayException;
import br.dev.s2w.jcomments.comment.service.domain.exception.ModerationRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.channels.ClosedChannelException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            SocketTimeoutException.class,
            ConnectException.class,
            ClosedChannelException.class
    })
    public ProblemDetail handle(IOException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.GATEWAY_TIMEOUT);

        problemDetail.setTitle("Gateway timeout");
        problemDetail.setDetail(e.getMessage());
        problemDetail.setType(URI.create("/errors/gateway-timeout"));

        return problemDetail;
    }

    @ExceptionHandler(ModerationClientBadGatewayException.class)
    public ProblemDetail handle(ModerationClientBadGatewayException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);

        problemDetail.setTitle("Bad Gateway");
        problemDetail.setDetail("An unexpected error occurred while communicating with the moderation service.");
        problemDetail.setType(URI.create("/errors/bad-gateway"));

        return problemDetail;
    }

    @ExceptionHandler(ModerationRejectedException.class)
    public ProblemDetail handle(ModerationRejectedException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);

        problemDetail.setTitle("Unprocessable Entity");
        problemDetail.setDetail(e.getMessage());
        problemDetail.setType(URI.create("/errors/unprocessable-entity"));

        return problemDetail;
    }

}
