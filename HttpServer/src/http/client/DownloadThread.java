package http.client;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import http.common.HttpHeader;
import http.common.HttpRequest;
import http.common.HttpResponse;

public class DownloadThread extends Thread
{
	public static enum DownloadState {NEW, DOWNLOADING, WAITING, ERROR, NOT_FOUND};
	
	private static final String USER_AGENT = "HttpClient Downloader";
	private static final long RETRY_WAIT_TIME = 5000;
	
	private Socket socket = null;
	private HttpRequest request;
	private HttpResponse response;
	private URL path;
	private boolean done = false;
	
	private String savePath;
	private String fileName;
	private String extName;
	
	private String urlName;
	private int fileSize;
	private DownloadState currentState;
	
	public DownloadThread(URL path, String savePath)
	{
		this.path = path;
		this.urlName = path.toExternalForm();
		this.currentState = DownloadState.NEW;
		this.fileName = path.getPath().substring(path.getPath().lastIndexOf("/") + 1, path.getPath().lastIndexOf("."));
		this.extName = path.getPath().substring(path.getPath().lastIndexOf(".") + 1, path.getPath().length());
		this.savePath = savePath;
		
		this.buildRequest();
		this.openConnection();
	}
	
	// Construit la requête pour tenter de récupérer le fichier demandé
	private void buildRequest()
	{
		this.request = new HttpRequest();
		this.request.getHeader().setMethod("GET");
		this.request.getHeader().setFullPath(this.path.getFile());
		this.request.getHeader().setProtocol("HTTP/1.1");
		
		this.request.getHeader().setField("Host", this.path.getHost());
		this.request.getHeader().setField("User-Agent", DownloadThread.USER_AGENT);
		this.request.getHeader().setField("Accept", "*/*");
	}
	
	private void openConnection()
	{
		try
		{
			this.socket = new Socket(this.path.getHost(), (this.path.getPort() != -1) ? this.path.getPort() : this.path.getDefaultPort());
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void run()
	{
		try
		{
			boolean retry = true;
			
			do
			{
				if (retry)
				{
					this.openConnection();
				}
				
				if (this.request.send(this.socket.getOutputStream()))
				{
					// Attend de recevoir un header pour la réponse
					this.response = new HttpResponse();
					this.response.receiveHeader(this.socket.getInputStream());
					HttpHeader responseHeader = this.response.getHeader();
					
					// Tente de parser le header
					// TODO : Probleme dans le parsing ...
					boolean requestIsGood = responseHeader.parseResponseHeader();
					
					System.out.println(String.format("[D] Thread %d (Réponse)", Thread.currentThread().getId()));
					System.out.print(responseHeader.getText());
					
					// Si la requête est bien formée
					if (requestIsGood)
					{
						this.fileSize = Integer.parseInt(responseHeader.getField("Content-Length"));
						
						System.out.println(String.format("Taille du fichier : %d", this.fileSize));
						
						File f = new File(this.savePath + this.fileName + "." + this.extName);
						System.out.println(String.format("t1 : %s", f.getAbsoluteFile()));
						System.out.println(String.format("t2 : %s", f.getAbsolutePath()));
						
						if (f.exists())
						{
							int i = 1;
							while (f.exists())
							{
								f.renameTo(new File(this.savePath + this.fileName + " (" + i + ")." + this.extName));
								i++;
							}
						}
						
						this.response.setFileName(f.getAbsolutePath());
							
						// TODO : Verif Code + file exist on server
						
						this.response.read(this.socket.getInputStream());
							
						System.out.println(String.format("Download succeed. Saved : %s", f.getAbsoluteFile()));
						retry = false;
					}
				}
				
				if (retry)
				{
					System.out.println(String.format("Failed request. Retrying in %d seconds...", DownloadThread.RETRY_WAIT_TIME / 1000));
					
					this.sleepFor(DownloadThread.RETRY_WAIT_TIME);
				}
				
				this.closeConnection();
			}
			while (retry);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void closeConnection()
	{
		try
		{
			this.socket.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean isDone()
	{
		return this.done;
	}
	
	public String getUrl()
	{
		return this.urlName;
	}
	
	public int getSize()
	{
		return this.fileSize;
	}
	
	public void setCurrentState(DownloadState newState)
	{
		if (newState != null)
		{
			this.currentState = newState;
		}
	}
	
	public DownloadState getCurrentState()
	{
		return this.currentState;
	}
	
	private void sleepFor(long milliseconds)
	{
		try
		{
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}
