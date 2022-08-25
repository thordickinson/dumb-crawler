package com.thordickinson.webcrawler.api.filter;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.url.WebURL;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class FilterPage {
    private FilterUri uri;
    private Optional<String> contentType;

    public static FilterPage fromPage(Page page){
        var url = FilterUri.fromUrl(page.getWebURL().getURL().toLowerCase());
        return new FilterPage(url, Optional.ofNullable(page.getContentType()));
    }

    public static FilterPage fromUri(WebURL uri){
        return new FilterPage(FilterUri.fromUrl(uri.getURL().toLowerCase()), Optional.empty());
    }
    public static FilterPage fromUri(String uri){
        return new FilterPage(FilterUri.fromUrl(uri), Optional.empty());
    }
}
