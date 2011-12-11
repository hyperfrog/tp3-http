import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import http.server.SocketListener;

public class ServerApp
{
	private final static String DEFAULT_CONFIG_FILE = "server.cfg";

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String userDir = System.getProperties().getProperty("user.dir");
		String fileSeparator = System.getProperties().getProperty("file.separator");

		String serverPath = "";
		String siteFolder = "";
		String ipAddress = "";
		int portNum = 0;
		int duration = 0;

		// Read properties file.
		Properties properties = new Properties();
		
		InputStream is = null;
		
		try
		{
			is = new FileInputStream((args.length > 0) ? args[0] : "");
			System.out.println("Utilisation du fichier de configuation \"" + args[0] + "\" spécifié dans la ligne de commande.");
		}
		catch (FileNotFoundException e)
		{
			if (args.length > 0)
			{
				System.out.println("Impossible de trouver le fichier de configuation \"" + args[0] + "\" spécifié dans la ligne de commande.");
			}
			try
			{
				is = new FileInputStream(userDir + fileSeparator + ServerApp.DEFAULT_CONFIG_FILE);
				System.out.println("Utilisation du fichier de configuation \"" + ServerApp.DEFAULT_CONFIG_FILE + "\" trouvé dans \"" + userDir + "\".");
			}
			catch (FileNotFoundException e2)
			{
				System.out.println("Impossible de trouver un fichier de configuation nommé \"" + ServerApp.DEFAULT_CONFIG_FILE + "\" dans \"" + userDir + "\".");
				is = ServerApp.class.getResourceAsStream("cfg/" + ServerApp.DEFAULT_CONFIG_FILE);
				System.out.println("Utilisation de la configuation par défaut.");
			}
		}
		
		if (is != null)
		{
			try
			{
				properties.load(is);
				serverPath = properties.getProperty("server_path");
				siteFolder = properties.getProperty("site_folder");
				
				try
				{
					ipAddress = InetAddress.getByName(properties.getProperty("host")).getHostAddress();
				}
				catch (UnknownHostException e)
				{
					ipAddress = "127.0.0.1";
				}
				
				try 
				{
					portNum = Integer.parseInt(properties.getProperty("port_num"));
				}
				catch (NumberFormatException e)
				{
					portNum = 80;
				}
				
				try 
				{
					duration = Integer.parseInt(properties.getProperty("duration"));
				}
				catch (NumberFormatException e)
				{
					duration = 0;
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}			
		}
		
		System.out.println("Configuration : ");
		System.out.println("Chemin absolu des fichiers du serveur : " + serverPath);
		System.out.println("Chemin relatif du site Web : " + siteFolder);
		System.out.println("Adresse IP : " + ipAddress);
		System.out.println("Numéro du port : " + portNum);
		System.out.print("Durée : ");
		if (duration > 0)
		{
			System.out.println(duration + " secondes");
		}
		else
		{
			System.out.println("indéterminée");
		}
			
		System.out.println();

		SocketListener sl = new SocketListener(serverPath, siteFolder, ipAddress, portNum);
		Thread slt = new Thread(sl);
		slt.start();
		
		System.out.println("Serveur démarré.");
		
		try
		{
			if (duration > 0)
			{
				int t = 0;
				
				while (t < 2 * duration && slt.isAlive())
				{
					Thread.sleep(500);
					t++;
				}
				
				sl.stop();
			}
			else
			{
				slt.join();
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		System.out.println("Serveur arrêté.");
	}
}
