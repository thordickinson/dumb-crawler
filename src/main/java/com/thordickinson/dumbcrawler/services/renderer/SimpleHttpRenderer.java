package com.thordickinson.dumbcrawler.services.renderer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;


import com.thordickinson.dumbcrawler.util.AbstractCrawlingComponent;

public class SimpleHttpRenderer extends AbstractCrawlingComponent implements HtmlRenderer {

    public SimpleHttpRenderer() {
        super("simpleHttpRenderer");
    }

    @Override
    public HtmlRenderResponse renderHtml(String url) {
       try{
        return render(url);
       }catch(Exception e){
        return HtmlRenderResponse.error(500, e);
       }
    }

    private HtmlRenderResponse render(String url) throws IOException  {
        URL obj = URI.create(url).toURL();
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
  
        // optional default is GET
        con.setRequestMethod("GET");
  
        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
         con.setRequestProperty("Accept-Charset", "ISO-8859-1"); 
        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);
  
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
  
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return  HtmlRenderResponse.success(response.toString());
    }
}