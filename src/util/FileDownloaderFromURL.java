package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.FileUtils;

/*
 * URL 페이지를 파싱한 후에 다운로드 파일 링크를 얻어와서 파일을 다운로드한다.
 */
public class FileDownloaderFromURL {

    public static void downloadFile(String url, File file) throws Exception {
        URL downPage = new URL(url);
        URLConnection yc = downPage.openConnection();
        BufferedReader in = new BufferedReader(
                                new InputStreamReader(
                                yc.getInputStream()));
        String inputLine;
        String result;
        
        while ((inputLine = in.readLine()) != null) { 
            if (inputLine.contains("gsSaveFileLink")) {
                String[] v1, v2;
                v1 = inputLine.split("\"");
                v2 = v1[1].split("\"");
                result = v2[0];
                System.out.println(result);
                
                FileUtils.copyURLToFile(new URL(url), file);
                break;
            }
        }
        in.close();
    }
	
}
