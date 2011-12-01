package http.server;

import http.common.HttpHeader;
import http.common.HttpRequest;
import http.common.HttpResponse;
import http.server.event.RequestEvent;
import http.server.event.RequestEventProcessor;

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
	
	/**
	 * @param client
	 * @param serverPath
	 * @param siteRoot
	 * @param mimeTypes
	 * @param ep
	 */
	public HttpServer(Socket client, String serverPath, String siteRoot, Map<String, String> mimeTypes, RequestEventProcessor ep)
	{
		this.socket = client;
		File file = new File(serverPath);
		this.serverPath = file.isDirectory() ? file.getAbsolutePath() + "\\" : "";
		this.siteRoot = siteRoot;
		this.mimeTypes = mimeTypes;
		this.evtProcessor = ep;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			// Crée une requête d'entrée
			this.request = new HttpRequest();
			// Attend de recevoir un header pour la requête
			this.request.receiveHeader(this.socket.getInputStream());
			HttpHeader requestHeader = this.request.getHeader();
			
			// Tente de parser le header
			boolean requestIsGood = requestHeader.parseRequestHeader();
			
			System.out.println(String.format("Thread %d (Requête)", Thread.currentThread().getId()));
			System.out.print(requestHeader.getText());
			
			this.response = new HttpResponse();
			HttpHeader responseHeader = this.response.getHeader();

			// Si la requête est bien formée
			if (requestIsGood)
			{
				// Envoie l'évènement de requête reçue
				RequestEvent evt = new RequestEvent(this);
				this.evtProcessor.requestEventReceived(evt);

				// Si l'évènement n'a pas été annulé 
				if (!evt.cancel) 
				{
					// Si la requête utilise la méthode GET 
					if (requestHeader.getMethod().equals("GET"))
					{
						// La réponse sera «cachable» puisqu'on s'apprête à servir un fichier du système de fichiers 
						this.response.setCacheable(true);

						File f = new File(this.serverPath + this.siteRoot + requestHeader.getPath());

						if (f.exists()) 
						{
							this.response.setFileName(f.getAbsolutePath());

							// If dateTime1 is NOT earlier than dateTime2 -> 304 Not Modified
							if (requestHeader.getField("If-Modified-Since") != null 
									&& !DateUtil.parseDate(requestHeader.getField("If-Modified-Since")).before(
											DateUtil.parseDate(DateUtil.formatDate(new Date(f.lastModified())))))
							{
								responseHeader.setStatusCode(304);
							}
							else
							{
								responseHeader.setStatusCode(200); // OK

								Pattern pFileExtension = Pattern.compile("\\.([^\\.]+)\\Z");
								Matcher mFileExtension = pFileExtension.matcher(this.response.getFileName());

								if (mFileExtension.find()) 
								{
									String fileMimeType = this.mimeTypes.get(mFileExtension.group(1));
									responseHeader.setField("Content-Type", fileMimeType);
								}

								responseHeader.setField("Cache-Control", "public");
								responseHeader.setField("Last-Modified", DateUtil.formatDate(new Date(f.lastModified())));
								responseHeader.setField("Content-Length", f.length() + "");
							}
						}
						else
						{
							responseHeader.setStatusCode(404); // Not Found
							responseHeader.setField("Content-Length", "0");

							if (requestHeader.accepts("text/html"))
							{
								File f404 = new File(this.serverPath + "error_404.htm");

								if (f404.exists())
								{
									this.response.setFileName(f404.getAbsolutePath());

									responseHeader.setField("Content-Type", "text/html");
									responseHeader.setField("Content-Length", f404.length() + "");
								}
							}
						}
					}
				}
			}
			else
			{
				responseHeader.setStatusCode(400); // Bad Request
			}
			
			// Si capable de créer le header
			if (this.response.makeHeader())
			{
				System.out.println(String.format("Thread %d (Réponse)", Thread.currentThread().getId()));
				System.out.print(responseHeader.getText());

				// Envoie la réponse
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
