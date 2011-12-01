package http.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import http.common.HttpRequest;
import http.common.HttpResponse;

public class DownloadThread extends Thread
{
	private static final String USER_AGENT = "HttpClient Downloader";
	private static final int KB_PER_SECOND = 50;
	
	private Socket socket = null;
	private HttpRequest request;
	private HttpResponse response;
	
	private String fileName;
	
	private boolean paused = false;
	private boolean done = false;
	
	public DownloadThread(String path)
	{
		try
		{
			URL url = new URL(path);
			
			// TODO : Temporaire
			this.fileName = url.getPath().substring(url.getPath().lastIndexOf("/") + 1, url.getPath().length());
			System.out.println(this.fileName);
			
			this.request = new HttpRequest();
			this.request.getHeader().setMethod("GET");
			this.request.getHeader().setFullPath(url.getFile());
			this.request.getHeader().setProtocol("HTTP/1.1");
			
			this.request.getHeader().setField("Host", url.getHost());
			this.request.getHeader().setField("User-Agent", DownloadThread.USER_AGENT);
			this.request.getHeader().setField("Accept", "*/*");
			
			this.socket = new Socket(url.getHost(), (url.getPort() != -1) ? url.getPort() : url.getDefaultPort());
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
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
			if (this.request.send(this.socket.getOutputStream()))
			{
				// TODO : Ne fonctionne pas ... et je ne comprends pas pourquoi ...
				FileOutputStream fos = new FileOutputStream("C:\\Test\\" + this.fileName);
					
				byte[] buf = new byte[1024];
				int len;

				while ((len = this.socket.getInputStream().read(buf)) > 0)
				{
					System.out.println("Downloading...");

					fos.write(buf, 0, len);

					try
					{
						Thread.sleep(1000/KB_PER_SECOND);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}

				fos.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
//		while (!this.isDone())
//		{
//			synchronized (this)
//			{
//				while (this.paused)
//				{
//					try
//					{
//						this.wait();
//					}
//					catch (Exception e)
//					{
//					}
//				}
//			}
//		}
	}
	
	public boolean isDone()
	{
		return this.done;
	}
	
	public void pauseDownload()
	{
		this.paused = true;
	}

	public void resumeDownload()
	{
		this.paused = false;
		
		synchronized (this)
		{
			this.notifyAll();
		}
	}
	
	public boolean isPaused()
	{
		return this.paused;
	}
}
