import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import http.server.Dispatcher;

/**
 * La classe ServerApp sert à lancer le thread répartiteur de requêtes.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class ServerApp
{
	private final static String DEFAULT_CONFIG_FILE = "server.cfg";

	/**
	 * Point d'entrée du serveur. Lance le thread répartiteur de requêtes et attend sa mort.
	 * 
	 * @param args Facultatif : chemin du fichier de configuration à utiliser 
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

		Properties properties = new Properties();
		InputStream is = null;
		try
		{
			// Lit le fichier de configuration spécifié dans la ligne de commange
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
				// Lit le fichier de configuration se trouvant dans le répertoire courant
				is = new FileInputStream(userDir + fileSeparator + ServerApp.DEFAULT_CONFIG_FILE);
				System.out.println("Utilisation du fichier de configuation \"" + ServerApp.DEFAULT_CONFIG_FILE + "\" trouvé dans \"" + userDir + "\".");
			}
			catch (FileNotFoundException e2)
			{
				// Lit le fichier de configuration interne
				System.out.println("Impossible de trouver un fichier de configuation nommé \"" + ServerApp.DEFAULT_CONFIG_FILE + "\" dans \"" + userDir + "\".");
				is = ServerApp.class.getResourceAsStream("cfg/" + ServerApp.DEFAULT_CONFIG_FILE);
				System.out.println("Utilisation de la configuation par défaut.");
			}
		}
		
		// Analyse les paramètres du fichier de configuration
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
		
		// Indique la configuration utilisée  
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

		// Démarre le répartiteur de requêtes
		Dispatcher dispatcher = new Dispatcher(serverPath, siteFolder, ipAddress, portNum);
		Thread dispatcherThread = new Thread(dispatcher);
		dispatcherThread.start();
		
		System.out.println("Serveur démarré.");
		
		try
		{
			// Si une durée d'exécution a été spécifiée
			if (duration > 0)
			{
				int t = 0;
				
				// Attend la fin du délai ou la mort du thread 
				while (t < 2 * duration && dispatcherThread.isAlive())
				{
					Thread.sleep(500);
					t++;
				}
				// Demande gentiment au répartiteur de se suicider
				dispatcher.stop();
			}
			else // Attend la mort naturelle du répartiteur
			{
				dispatcherThread.join();
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		System.out.println("Serveur arrêté.");
	}
}
