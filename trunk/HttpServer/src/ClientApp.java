
import http.client.AppFrame;

/**
 * La classe MainApp contient une m�thode main(), qui constitue le point d'entr�e du client.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class ClientApp
{
	/**
	 * Point d'entr�e du programme.
	 * 
	 * @param args param�tres re�us en ligne de commande (non utilis�s) 
	 */
	public static void main(String[] args)
	{
		AppFrame appFrame = new AppFrame();
		appFrame.setVisible(true);
	}
}
