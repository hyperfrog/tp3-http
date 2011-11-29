package http;

public class MainApp
{
	private final static int DURATION = 0;
	private final static String SERVER_PATH = "/C:/Users/Public/Documents/HTTP_Server/";
	private final static String SITE_FOLDER = "Javadoc";
	private final static String IP_ADDRESS = "127.0.0.1"; 
	private final static int PORT_NUM = 80;

	/**
	 * @param args
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args)
	{
		SocketListener sl = new SocketListener(SERVER_PATH, SITE_FOLDER, IP_ADDRESS, PORT_NUM);
		Thread slt = new Thread(sl);
		slt.start();
		
		System.out.println("Serveur démarré.");
		
		try
		{
			if (DURATION > 0)
			{
				Thread.sleep(DURATION * 1000);
				sl.stop();
				System.out.println(String.format("%d secondes écoulées.", DURATION));
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

