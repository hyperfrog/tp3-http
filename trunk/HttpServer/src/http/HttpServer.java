package http;

import http.event.RequestEvent;
import http.event.RequestEventProcessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.DateUtil;

public class HttpServer implements Runnable
{
	private Socket socket = null;
	private String serverPath;
	private String siteRoot = "";
	private Map<String, String> mimeTypes;
	private RequestEventProcessor evtProcessor;
	private HttpRequest request;
	private HttpResponse response;
	
	HttpServer(Socket client, String serverPath, String siteRoot, Map<String, String> mimeTypes, RequestEventProcessor ep)
	{
		this.socket = client;
		File file = new File(serverPath);

		this.serverPath = file.isDirectory() ? file.getAbsolutePath() + "\\" : "";
		this.siteRoot = siteRoot;
		this.mimeTypes = mimeTypes;
		this.evtProcessor = ep;
	}

	public void run()
	{
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

			String requestHeader = new String();
			String line;
			
			// Lit l'en-tête (header)
			do
			{
				line = in.readLine();
				requestHeader += line + "\n";

			} while (line != null && !line.isEmpty());

			this.request = new HttpRequest(requestHeader);
			this.response = new HttpResponse();

			RequestEvent evt = new RequestEvent(this, Thread.currentThread());
			
			this.evtProcessor.requestEventReceived(evt);

//			System.out.println(String.format("%d, %s", Thread.currentThread().getId(), Thread.currentThread().getName()));
//			System.out.print(requestHeader);

			if (!evt.cancel && this.request.getMethod().equals("GET")) 
			{
				this.response.setCacheable(true);

				File f = new File(this.serverPath + this.siteRoot + this.request.getPathName());

				if (f.exists()) 
				{
					this.response.setFileName(f.getAbsolutePath());
					
					// if dateTime1 is NOT earlier than dateTime2 -> 304 Not Modified
					if (this.request.getField("If-Modified-Since") != null 
						&& !DateUtil.parseDate(this.request.getField("If-Modified-Since")).before(
							DateUtil.parseDate(DateUtil.formatDate(new Date(f.lastModified())))))
					{
						this.response.setStatusCode(304);
					}
					else
					{
						this.response.setStatusCode(200);

						Pattern pFileExtension = Pattern.compile("\\.([^\\.]+)\\Z");
						Matcher mFileExtension = pFileExtension.matcher(this.response.getFileName());

						if (mFileExtension.find()) 
						{
							String fileMimeType = this.mimeTypes.get(mFileExtension.group(1));
							this.response.setField("Content-Type", fileMimeType);
						}

						this.response.setField("Cache-Control", "public");
						this.response.setField("Last-Modified", DateUtil.formatDate(new Date(f.lastModified())));
						this.response.setField("Content-Length", f.length() + "");
					}
				}
				else
				{
					this.response.setStatusCode(404);
					this.response.setField("Content-Length", "0");
					
					if (this.request.getAcceptList().contains("text/html"))
					{
						File f404 = new File(this.serverPath + "error_404.htm");

						if (f404.exists())
						{
							this.response.setFileName(f404.getAbsolutePath());

							this.response.setField("Content-Type", "text/html");
							this.response.setField("Content-Length", f404.length() + "");
						}
					}
				}
				this.response.makeHeader();
//				System.out.println(String.format("%d, %s", Thread.currentThread().getId(), Thread.currentThread().getName()));
				System.out.println(String.format("Thread %d (Réponse)", Thread.currentThread().getId()));
				System.out.print(this.response.getHeader());
				this.response.send(this.socket.getOutputStream());
			}
	        this.socket.close();
		}
		catch (IOException e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * @return the request
	 */
	public HttpRequest getRequest()
	{
		return request;
	}

	/**
	 * @return the response
	 */
	public HttpResponse getResponse()
	{
		return response;
	}

	/**
	 * @return the socket
	 */
	public Socket getSocket()
	{
		return socket;
	}
	
}
