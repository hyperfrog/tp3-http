package http;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.DateUtil;

class HttpServer implements Runnable
{
	private Socket socket;
	private String serverPath;
	private String siteRoot;
	private Map<String, String> mimeTypes;
	
	HttpServer(Socket clientSocket, String serverPath, String siteRoot, Map<String, String> mimeTypes)
	{
		this.socket = clientSocket;
		this.serverPath = serverPath;
		this.siteRoot = siteRoot;
		this.mimeTypes = mimeTypes;
	}

	public void run()
	{
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

			String requestHeader = new String();
			String line;
			
			do
			{
				line = in.readLine();
				requestHeader += line + "\n";

			} while (line != null && !line.isEmpty());

			System.out.print(requestHeader);

			HttpRequest request = new HttpRequest(requestHeader);

			if (request.getMethod().equals("GET")) 
			{
				HttpResponse response = new HttpResponse();

				response.setCacheable(true);

				File f = new File(this.serverPath + this.siteRoot + request.getPathName());

				if (f.exists()) 
				{
					response.setFileName(f.getAbsolutePath());
					
					// if dateTime1 is NOT earlier than dateTime2 -> 304 Not Modified
					if (request.getField("If-Modified-Since") != null 
						&& !DateUtil.parseDate(request.getField("If-Modified-Since")).before(
							DateUtil.parseDate(DateUtil.formatDate(new Date(f.lastModified())))))
					{
						response.setStatusCode(304);
					}
					else
					{
						response.setStatusCode(200);

						Pattern pFileExtension = Pattern.compile("\\.([^\\.]+)\\Z");
						Matcher mFileExtension = pFileExtension.matcher(response.getFileName());

						if (mFileExtension.find()) 
						{
							String fileMimeType = this.mimeTypes.get(mFileExtension.group(1));
							response.setField("Content-Type", fileMimeType);
						}

						response.setField("Cache-Control", "public");
						response.setField("Last-Modified", DateUtil.formatDate(new Date(f.lastModified())));
						response.setField("Content-Length", f.length() + "");
					}
				}
				else
				{
					response.setStatusCode(404);
					response.setField("Content-Length", "0");
					
					if (request.getAcceptList().contains("text/html"))
					{
						File f404 = new File(this.serverPath + "error_404.htm");

						if (f404.exists())
						{
							response.setFileName(f404.getAbsolutePath());

							response.setField("Content-Type", "text/html");
							response.setField("Content-Length", f404.length() + "");
						}
					}
				}
				response.makeHeader();
				System.out.print(response.getHeader());
				response.send(this.socket.getOutputStream());
			}
	        in.close();
	        this.socket.close();
		}
		catch (IOException e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
