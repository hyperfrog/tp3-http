package http.server;

import static util.BasicString.join;
import static util.BasicString.split;
import http.common.BadHeaderException;
import http.common.HttpRequest;
import http.common.HttpRequestHeader;
import http.common.HttpResponse;
import http.common.HttpResponseHeader;
import http.common.TransferController;
import http.server.event.RequestEvent;
import http.server.event.RequestEventProcessor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.DateUtil;

/**
 * Cette classe mod�lise un serveur HTTP qui r�pond � une requ�te en s'ex�cutant dans un thread.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class HttpServerThread implements Runnable
{
	// Socket de la connexion li�e au thread
	private Socket socket = null;
	
	// Chemin absolu du dossier o� se trouvent les fichiers n�cessaires au serveur
	private String serverPath;
	
	// Chemin relatif du dossier o� se trouvent les fichiers du site Web � servir
	private String siteFolder;
	
	// Dictionnaire des extensions et des types MIME correspondants
	private Map<String, String> mimeTypes;
	
	// Objet traitant les �v�nements de requ�te re�ue
	private RequestEventProcessor evtProcessor;
	
	// Requ�te d'entr�e
	private HttpRequest request;
	
	// R�ponse de sortie
	private HttpResponse response;
	
	// Objet servant � contr�ler le t�l�chargement
	private TransferController tc;
	
	// S�parateur de noms de dossier propre � la plateforme
	private static final String FILE_SEP = System.getProperties().getProperty("file.separator");

	/**
	 * Construit une instance de serveur HTTP s'ex�cutant dans un thread.  
	 * 
	 * @param client socket ouvert pour la communication avec le client
	 * @param serverPath chemin absolu du dossier o� se trouvent les fichiers n�cessaires au serveur
	 * @param siteFolder chemin relatif du dossier o� se trouvent les fichiers du site Web � servir
	 * @param mimeTypes dictionnaire des extensions et des types MIME correspondants 
	 * @param ep objet traitant les �v�nements de requ�te re�ue
	 */
	public HttpServerThread(Socket client, String serverPath, String siteFolder, Map<String, String> mimeTypes, RequestEventProcessor ep)
	{
		this.socket = client;
		
		File f = new File(serverPath);
		this.serverPath = f.isDirectory() ? f.getAbsolutePath() : ".";
		this.serverPath += FILE_SEP;
		
		f = new File(this.serverPath + siteFolder);
		this.siteFolder = f.isDirectory() ? siteFolder : "www";
		
		this.mimeTypes = mimeTypes;

		this.evtProcessor = ep;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		// Cr�e la requ�te d'entr�e
		this.request = new HttpRequest();
		
		HttpRequestHeader requestHeader = this.request.getHeader();

		try
		{
			// Attend de recevoir un header pour la requ�te
			requestHeader.receive(this.socket.getInputStream());
			
			this.response = new HttpResponse();
			HttpResponseHeader responseHeader = this.response.getHeader();

			// Limite le transfert � 1000 Ko/s
			this.tc = new TransferController(1000, false);

			try
			{
				// Tente de parser le header
				requestHeader.parse();
				
				System.out.println(String.format("Transaction %s (Requ�te)", Thread.currentThread().getName()));
				System.out.print(requestHeader.getText());
			
				// Envoie un �v�nement de requ�te re�ue pour permettre d'effectuer un traitement diff�rent
				RequestEvent evt = new RequestEvent(this);
				this.evtProcessor.requestEventReceived(evt);

				// Si l'�v�nement n'a pas �t� annul� 
				if (!evt.cancel) 
				{
					// Si la requ�te utilise la m�thode GET ou HEAD
					if (requestHeader.getMethod().equals("GET") || requestHeader.getMethod().equals("HEAD"))
					{
						if (requestHeader.getMethod().equals("HEAD"))
						{
							this.response.setContentSendable(false);
						}
						
						// La r�ponse sera �cachable� puisqu'on s'appr�te � servir un fichier du syst�me de fichiers 
						responseHeader.setCacheable(true);
						
						String filePath = this.serverPath + this.siteFolder + requestHeader.getPath();
						
						// Remplace les / par autre chose au besoin
						if (FILE_SEP != "/")
						{
							filePath = join(split(filePath, "/"), FILE_SEP);
						}

//						System.out.println("Fichier � servir : " + filePath);

						File f = new File(filePath);
						
						// Fichier inexistant ?
						if (!f.exists()) 
						{
							responseHeader.setStatusCode(404); // Not Found
						}
						// R�pertoire ? Permission lecture manquante ?
						else if (f.isDirectory() || !f.canRead()) 
						{
							responseHeader.setStatusCode(403); // Forbidden
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
					}
				}
			}
			catch (BadHeaderException e) // Pas capable d'analyser le header de la requ�te du client
			{
				responseHeader.setStatusCode(400); // Bad Request
			}
			
			// Si le socket est encore ouvert
			if (!this.socket.isClosed())
			{
				int statusCode = responseHeader.getStatusCode();

				// S'il s'agit d'un code d'erreur
				if (statusCode >= 400)
				{
					File fError = new File(this.serverPath + "error_" + statusCode + ".htm");

					// Si un fichier de r�ponse existe pour ce type d'erreur  
					if (fError.exists())
					{
						this.response.setFileName(fError.getAbsolutePath());

						responseHeader.setField("Content-Type", "text/html");
						responseHeader.setField("Content-Length", fError.length() + "");
					}
					else if (responseHeader.getField("Content-Length") == null)
					{
						responseHeader.setField("Content-Length", "0");
					}
				}

				try
				{
					System.out.println(String.format("Transaction %s (R�ponse)", Thread.currentThread().getName()));
					responseHeader.make();
					
					System.out.print(responseHeader.getText());

					// Envoie la r�ponse
					this.response.send(this.socket.getOutputStream(), this.tc);
				}
				catch (BadHeaderException e1) // Pas capable de cr�er le header de r�ponse.
				{
					try // Essaie de cr�er une nouvelle r�ponse avec le code 500 
					{
						this.response = new HttpResponse();
						response.setContent(e1.getMessage().getBytes());
						
						responseHeader = this.response.getHeader();
						responseHeader.setStatusCode(500); // Internal Server Error
						responseHeader.make();
						
						System.out.print(responseHeader.getText());

						// Envoie la r�ponse
						this.response.send(this.socket.getOutputStream(), this.tc);
					}
					catch (BadHeaderException e2) // Pas capable..
					{
						System.out.println("Erreur 500");
						
						// Envoie une r�ponse �cod�e dur�  directement sur le socket. 
						OutputStreamWriter osw = new OutputStreamWriter(this.socket.getOutputStream());
						osw.write("HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\n\r\n");
						osw.flush();
					}
				}
			}
		}
		catch (IOException e)
		{
			System.err.println("Connexion interrompue.");
//			System.err.println("Erreur d'E/S : " + e.getMessage());
//			e.printStackTrace();
		}
		finally
		{
			if (!this.socket.isClosed())
			{
				try { this.socket.close(); } catch (IOException unused) {}
			}
		}
	}
	
	/**
	 * Permet d'arr�ter un transfert.
	 */
	public void stop()
	{
		this.tc.stopped = true;
	}

	/**
	 * Retourne l'objet request de cette instance du serveur.
	 * 
	 * @return objet request de cette instance du serveur
	 */
	public HttpRequest getRequest()
	{
		return this.request;
	}

	/**
	 * Retourne l'objet response de cette instance du serveur.
	 * 
	 * @return objet response de cette instance du serveur
	 */
	public HttpResponse getResponse()
	{
		return this.response;
	}

	/**
	 * Retourne le socket associ� � cette instance de serveur.
	 * 
	 * @return socket associ� � cette instance de serveur
	 */
	public Socket getSocket()
	{
		return this.socket;
	}
}
