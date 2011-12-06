package http.server;

import http.common.HttpRequest;
import http.common.HttpRequestHeader;
import http.common.HttpResponse;
import http.common.HttpResponseHeader;
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
			// Cr�e une requ�te d'entr�e
			this.request = new HttpRequest();
			// Attend de recevoir un header pour la requ�te
			HttpRequestHeader requestHeader = this.request.getHeader();
			requestHeader.receive(this.socket.getInputStream());
			
			// Tente de parser le header
			boolean requestIsGood = requestHeader.parse();
			
			System.out.println(String.format("Thread %d (Requ�te)", Thread.currentThread().getId()));
			System.out.print(requestHeader.getText());
			
			this.response = new HttpResponse();
			HttpResponseHeader responseHeader = this.response.getHeader();

			// Si la requ�te est bien form�e
			if (requestIsGood)
			{
				// Envoie l'�v�nement de requ�te re�ue
				RequestEvent evt = new RequestEvent(this);
				this.evtProcessor.requestEventReceived(evt);

				// Si l'�v�nement n'a pas �t� annul� 
				if (!evt.cancel) 
				{
					// Si la requ�te utilise la m�thode GET 
					if (requestHeader.getMethod().equals("GET") || requestHeader.getMethod().equals("HEAD"))
					{
						if (requestHeader.getMethod().equals("HEAD"))
						{
							this.response.setContentSendable(false);
						}
						
						// La r�ponse sera �cachable� puisqu'on s'appr�te � servir un fichier du syst�me de fichiers 
						this.response.setCacheable(true);

						File f = new File(this.serverPath + this.siteRoot + requestHeader.getPath());

						// Fichier inexistant ?
						if (!f.exists()) 
						{
							responseHeader.setStatusCode(404); // Not Found
							responseHeader.setField("Content-Length", "0");
						}
						// R�pertoire ? Permission lecture manquante ?
						else if (f.isDirectory() || !f.canRead()) 
						{
							responseHeader.setStatusCode(403); // Forbidden
							responseHeader.setField("Content-Length", "0");
						}
						else
						{
							this.response.setFileName(f.getAbsolutePath());

							// Si date demand�e n'est pas ant�rieure � date du fichier
							if (requestHeader.getField("If-Modified-Since") != null 
									&& !DateUtil.parseDate(requestHeader.getField("If-Modified-Since")).before(
											DateUtil.parseDate(DateUtil.formatDate(new Date(f.lastModified())))))
							{
								responseHeader.setStatusCode(304); // Not Modified
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
					}
					else // POST, PUT, DELETE, TRACE, OPTIONS, CONNECT, PATCH
					{
						responseHeader.setStatusCode(501); // Not Implemented
						responseHeader.setField("Content-Length", "0");
					}
				}
			}
			else
			{
				responseHeader.setStatusCode(400); // Bad Request
				responseHeader.setField("Content-Length", "0");
			}
			
			// Si capable de cr�er le header
			if (responseHeader.make())
			{
				System.out.println(String.format("Thread %d (R�ponse)", Thread.currentThread().getId()));
				System.out.print(responseHeader.getText());

				int statusCode = responseHeader.getStatusCode(); 
				
				if (statusCode >= 400)
				{
					File fError = new File(this.serverPath + "error_" + statusCode + ".htm");

					if (fError.exists())
					{
						this.response.setFileName(fError.getAbsolutePath());

						responseHeader.setField("Content-Type", "text/html");
						responseHeader.setField("Content-Length", fError.length() + "");
					}
				}
				
				// Envoie la r�ponse
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
