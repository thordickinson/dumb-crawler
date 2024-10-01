package com.thordickinson.dumbcrawler.services.renderer;

import java.util.Optional;

public record HtmlRenderResponse(int statusCode, Optional<String> body, Optional<Exception> ex) {
    static HtmlRenderResponse error(int statusCode, Exception error) {
        return new HtmlRenderResponse(statusCode, Optional.empty(), Optional.ofNullable(error));
    }

    static HtmlRenderResponse error(int statusCode) {
        return new HtmlRenderResponse(statusCode, Optional.empty(), Optional.empty());
    }
    static HtmlRenderResponse success(String body) {
        return new HtmlRenderResponse(200, Optional.of(body), Optional.empty());
    }
}
