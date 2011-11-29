package client;

public class DownloadThread extends Thread
{
	private String path;
	private boolean paused = false;
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
			synchronized (this)
			{
				while (this.paused)
				{
					try
					{
						this.wait();
					}
					catch (Exception e)
					{
					}
				}
				
				System.out.println("[ " + this.path + " ] Downloading ...");
			}
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
