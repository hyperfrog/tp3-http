
import http.client.AppFrame;

/**
 * La classe MainApp contient une méthode main(), qui constitue le point d'entrée du client.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class ClientApp
{
	/**
	 * Point d'entrée du programme.
	 * 
	 * @param args paramètres reçus en ligne de commande (non utilisés) 
	 */
	public static void main(String[] args)
	{
		AppFrame appFrame = new AppFrame();
		appFrame.setVisible(true);
	}
}
