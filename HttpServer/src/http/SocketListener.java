package http;

import static util.BasicString.stringToMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import http.event.RequestEvent;
import http.event.RequestEventProcessor;

public class SocketListener implements RequestEventProcessor, Runnable
{
	private final String serverPath;
	private final String siteFolder;
	private final String ipAddress; 
	private final int portNum;
	private final static String MIME_TYPES_FILE = "mime_types.txt"; 
	private final static int BACKLOG = 10;
	private final static int MAX_THREADS = 10;
	private static Map<String, String> mimeTypes = null;
	
	private volatile Thread runThread;
	
	public SocketListener(String serverPath, String siteFolder, String ipAddress, int portNum)
	{
		this.serverPath = serverPath;
		this.siteFolder = siteFolder;
		this.ipAddress = ipAddress;
		this.portNum = portNum;
		
		if (SocketListener.mimeTypes == null)
		{
			// Read MIME types
			SocketListener.mimeTypes = new HashMap<String, String>();

			File mtFile = new File(serverPath + MIME_TYPES_FILE);
			FileInputStream fis = null;
			try
			{
				// Read the file   
				fis = new FileInputStream(mtFile);
				byte[] mtData = new byte[(int) mtFile.length()];
				fis.read(mtData);
				SocketListener.mimeTypes = stringToMap(new String(mtData), "\n", "=", true);
			}
			catch (IOException e)
			{
				System.err.println("Problem reading MIME types: " + e.getMessage());
			}
			finally
			{
				if (fis != null)
				{
					try { fis.close(); } catch (IOException unused) {}
				}
			}
		}
	}
	
	public void stop()
	{
		Thread tmpRunThread = this.runThread;
		this.runThread = null;
		if (tmpRunThread != null)
		{
			tmpRunThread.interrupt();
		}
	}

	public void run()
	{
        Thread myThread = Thread.currentThread();
        this.runThread = myThread;
        
		try
		{
			ServerSocket listener = new ServerSocket(portNum, BACKLOG, InetAddress.getByName(ipAddress));
			listener.setSoTimeout(100);
			
			Socket clientSocket;

			while (myThread == this.runThread) 
			{
				try
				{
					clientSocket = listener.accept();
					int nbThreads = Thread.activeCount();
					
					System.out.print(String.format("Il y a présentement %d thread(s) actif(s).\n", nbThreads));
					
					while (nbThreads >= MAX_THREADS)
					{
						try { Thread.sleep(50); } catch (InterruptedException unused) {}
					}

					HttpServer server = new HttpServer(clientSocket, serverPath, siteFolder, SocketListener.mimeTypes, this);
					Thread t = new Thread(server);
					t.start();
				}
				catch (SocketTimeoutException ste) {}
			}
		}
		catch (IOException e)
		{
			System.out.println("IOException on socket listen: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Override
	public void requestEventReceived(RequestEvent evt)
	{
		System.out.println(String.format("Thread %d (Requête)", Thread.currentThread().getId()));
		System.out.print(((HttpServer)evt.getSource()).getRequest().getHeader());
		
		HttpServer s = ((HttpServer)evt.getSource());
		HttpRequest req = s.getRequest();
		HttpResponse res = s.getResponse();
		
		if (req.getPathName().startsWith("/haha.html"))
		{
			evt.cancel = true;

			res.setStatusCode(200);

			String value = req.getParam("toto");
			if (value != null)
			{
				res.setField("Content-Length", value.length() + "");
				res.setContent(value, Charset.forName("ISO-8859-1"));
			}
			else
			{
				res.setField("Content-Length", "0");
			}
				
			res.makeHeader();
			try
			{
				res.send(s.getSocket().getOutputStream());
//				s.getSocket().close();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
