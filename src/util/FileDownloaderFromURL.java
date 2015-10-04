package util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import debug.Debug;
import debug.MyConstants;

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
                
//                FileUtils.copyURLToFile(new URL(url), file);
                downloadFileUtil(result, file);
				
                break;
            }
        }
        in.close();
    }

	public static void fileCombiner(String fname) {
		Debug.print("FileDownloaderFromURL", "fileCombiner", "Make a file: " + fname);
		
		// 다운로드 폴더에 분할된 파일을 모아 하나로 만든다.
		// 디렉토리에 있는 분할 파일 리스트 구성
		String[] fileList = new String[MyConstants.MAXIMUM_DIVISION_CNT];
		
		String downloadDir = System.getenv("HOME") + File.separator + "Downloads";
		File folder = new File(downloadDir);
		for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            // Skip
	        } else {
	        	String entry_name = fileEntry.getName();
	        	
	    		Debug.print("FileDownloaderFromURL", "fileCombiner", "entry name: " + fileEntry.getName());

	        	if (entry_name.substring(0, entry_name.length() - 2).compareTo(fname) == 0) {
	        		int idx = Integer.parseInt(entry_name.substring(entry_name.length() - 1));
	        		fileList[idx] = entry_name;
	        	}
	        }
	    }
		
		int BUFFER_SIZE = 8192;
		int bytesRead;
		byte[] buffer = new byte[BUFFER_SIZE];
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		try {
			fos = new FileOutputStream(new File(downloadDir + File.separator + fname));
			bos = new BufferedOutputStream(fos);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		// 완성 후 하나로 합친다.
		for (int i = 0; i < MyConstants.MAXIMUM_DIVISION_CNT; i++) {
			System.out.println(i + " is " + fileList[i]);
			if (fileList[i] == null) {
				break;
			}
			else {
				try {
					File fp = new File(downloadDir + File.separator + fileList[i]);
					FileInputStream fis = new FileInputStream(fp);
					BufferedInputStream bis = new BufferedInputStream(fis); 
					while ( (bytesRead = bis.read(buffer)) > 0 ) {
						bos.write(buffer, 0, bytesRead);
					}
					bos.flush();
					bis.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void downloadFileUtil(String fileURL, File file)
            throws IOException {
		int BUFFER_SIZE = 8192;
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();
 
        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = "";
            String disposition = httpConn.getHeaderField("Content-Disposition");
            String contentType = httpConn.getContentType();
            int contentLength = httpConn.getContentLength();
 
            if (disposition != null) {
                // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
            }
 
            System.out.println("Content-Type = " + contentType);
            System.out.println("Content-Disposition = " + disposition);
            System.out.println("Content-Length = " + contentLength);
            System.out.println("fileName = " + fileName);
 
            // opens input stream from the HTTP connection
            InputStream inputStream = httpConn.getInputStream();
            String saveFilePath = file.getAbsolutePath();
             
            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);
 
            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
 
            outputStream.close();
            inputStream.close();
 
            System.out.println("File downloaded");
        } else {
            System.out.println("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
    }
	
}
