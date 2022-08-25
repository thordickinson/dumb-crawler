package com.thordickinson.webcrawler.api.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class FilterUri {
    private String uri;
    private String protocol;
    private String host;
    private Integer port;
    private String path;
    private String query;
    public static FilterUri fromUrl(String uri){
        var parsed = URI.create(uri);
        return new FilterUri(uri, parsed.getScheme(), parsed.getHost(), parsed.getPort(), parsed.getPath(), parsed.getQuery());
    }
}
