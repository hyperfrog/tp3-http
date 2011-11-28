package http;

import http.event.RequestEvent;
import http.event.RequestEventProcessor;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
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
			this.request = new HttpRequest();
			this.request.receiveHeader(this.socket.getInputStream());
			
			System.out.println(String.format("Thread %d (Requête)", Thread.currentThread().getId()));
			System.out.print(this.request.getHeader().getText());

			this.response = new HttpResponse();

			RequestEvent evt = new RequestEvent(this, Thread.currentThread());
			
			this.evtProcessor.requestEventReceived(evt);

			if (!evt.cancel && this.request.getHeader().getMethod().equals("GET")) 
			{
				this.response.setCacheable(true);

				File f = new File(this.serverPath + this.siteRoot + this.request.getHeader().getPath());

				if (f.exists()) 
				{
					this.response.setFileName(f.getAbsolutePath());
					
					// if dateTime1 is NOT earlier than dateTime2 -> 304 Not Modified
					if (this.request.getHeader().getField("If-Modified-Since") != null 
						&& !DateUtil.parseDate(this.request.getHeader().getField("If-Modified-Since")).before(
							DateUtil.parseDate(DateUtil.formatDate(new Date(f.lastModified())))))
					{
						this.response.getHeader().setStatusCode(304);
					}
					else
					{
						this.response.getHeader().setStatusCode(200);

						Pattern pFileExtension = Pattern.compile("\\.([^\\.]+)\\Z");
						Matcher mFileExtension = pFileExtension.matcher(this.response.getFileName());

						if (mFileExtension.find()) 
						{
							String fileMimeType = this.mimeTypes.get(mFileExtension.group(1));
							this.response.getHeader().setField("Content-Type", fileMimeType);
						}

						this.response.getHeader().setField("Cache-Control", "public");
						this.response.getHeader().setField("Last-Modified", DateUtil.formatDate(new Date(f.lastModified())));
						this.response.getHeader().setField("Content-Length", f.length() + "");
					}
				}
				else
				{
					this.response.getHeader().setStatusCode(404);
					this.response.getHeader().setField("Content-Length", "0");
					
					if (this.request.getHeader().accepts("text/html"))
					{
						File f404 = new File(this.serverPath + "error_404.htm");

						if (f404.exists())
						{
							this.response.setFileName(f404.getAbsolutePath());

							this.response.getHeader().setField("Content-Type", "text/html");
							this.response.getHeader().setField("Content-Length", f404.length() + "");
						}
					}
				}
				this.response.makeHeader();
				System.out.println(String.format("Thread %d (Réponse)", Thread.currentThread().getId()));
				System.out.print(this.response.getHeader().getText());
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
