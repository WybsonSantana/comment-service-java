package br.dev.s2w.jcomments.comment.service.api.client;

import br.dev.s2w.jcomments.comment.service.api.model.ModerationRequest;
import br.dev.s2w.jcomments.comment.service.api.model.ModerationResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api/moderate")
public interface ModerationClient {
    @PostExchange
    ModerationResponse moderateComment(@RequestBody ModerationRequest request);
}
