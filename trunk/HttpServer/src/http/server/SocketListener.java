package http.server;

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
import java.util.Set;

import http.common.HttpRequest;
import http.common.HttpResponse;
import http.server.event.RequestEvent;
import http.server.event.RequestEventProcessor;

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
					
					System.out.print(String.format("Il y a présentement %d thread(s) actif(s).\n\n", nbThreads));
					
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
		HttpServer s = ((HttpServer)evt.getSource());
		HttpRequest request = s.getRequest();
		HttpResponse response = s.getResponse();
		
		if (request.getHeader().getPath().equals("/haha.html"))
		{
			evt.cancel = true;

			response.getHeader().setStatusCode(200);

			String content = new String();
			
			Set<String> keys = request.getHeader().getParamKeySet();
			
			for(String key : keys)
			{
				String value = request.getHeader().getParam(key);
//				if (value != null)
				{
					content += key + " = " + value + "\n";
				}
			}
			
//			res.setContent(content, Charset.forName("ISO-8859-1"));
			response.setContent(content, Charset.forName("UTF-8"));
//			res.setField("Content-Length", res.getContent().length + "");

			response.makeHeader();
			try
			{
				System.out.println(String.format("Thread %d (Réponse)", Thread.currentThread().getId()));
				System.out.print(response.getHeader().getText());
				response.send(s.getSocket().getOutputStream());
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
