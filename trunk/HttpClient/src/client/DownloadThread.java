package client;

public class DownloadThread extends Thread
{
	private String path;
	private boolean done = false;
	
	public DownloadThread(String path)
	{
		this.path = path;
	}
	
	@Override
	public void run()
	{
		// TODO : Faire quelque chose d'utile
		while (!this.isDone())
		{
			System.out.println("[ " + this.path + " ] Downloading ...");
		}
	}
	
	public String getPath()
	{
		return this.path;
	}
	
	public boolean isDone()
	{
		return this.done;
	}
}
