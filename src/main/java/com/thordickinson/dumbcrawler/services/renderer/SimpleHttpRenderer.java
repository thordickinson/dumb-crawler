package com.thordickinson.dumbcrawler.services.renderer;

import com.thordickinson.dumbcrawler.api.CrawlingTask;
import com.thordickinson.dumbcrawler.exceptions.CrawlingException;
import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class SimpleHttpRenderer extends AbstractCrawlingComponent implements HtmlRenderer {

    public SimpleHttpRenderer() {
        super("simpleHttpRenderer");
    }

    @Override
    public String renderHtml(CrawlingTask task) {
        try {
            return render(task);
        } catch (IOException e) {
            throw new CrawlingException(task, "HTML_RENDERING_ERROR", e.toString(), true, e);
        }
    }

    private String render(CrawlingTask task) throws IOException {
        URL obj = URI.create(transformUrl(task.url())).toURL();
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setConnectTimeout(10_000);
        con.setReadTimeout(30_000);

        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Accept-Charset", "ISO-8859-1");
        int responseCode = con.getResponseCode();
        if (responseCode != 200) {
            var retry = responseCode != 404;
            throw new CrawlingException(task, "INVALID_STATUS_CODE_" + responseCode, String.valueOf(responseCode), retry);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return response.toString();
    }

    protected String transformUrl(String original){
        return original;
    }
}
