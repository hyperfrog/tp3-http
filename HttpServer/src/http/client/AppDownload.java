package http.client;

import java.util.ArrayList;
import java.util.List;

public class AppDownload
{
	private List<DownloadThread> downloadsList;
	
	public AppDownload()
	{
		this.downloadsList = new ArrayList<DownloadThread>();
	}
	
	public boolean addDownload(String url)
	{
		DownloadThread dl = new DownloadThread(url);
		return this.downloadsList.add(dl); 
	}
	
	public void removeDownload(int pos)
	{
		if (this.downloadsList.size() > 0 && (pos >= 0 && pos <= this.downloadsList.size()))
		{
			if (this.downloadsList.get(pos).isAlive())
			{
				this.stopDownload(pos);
			}
			
			this.downloadsList.remove(pos);
		}
	}
	
	public void moveUp(int pos)
	{
		if (pos > 0 && pos <= this.downloadsList.size())
		{
			DownloadThread temp = this.downloadsList.get(pos - 1);
			this.downloadsList.set(pos - 1, this.downloadsList.get(pos));
			this.downloadsList.set(pos, temp);
		}
	}
	
	public void moveDown(int pos)
	{
		if (pos >= 0 && pos < this.downloadsList.size())
		{
			DownloadThread temp = this.downloadsList.get(pos + 1);
			this.downloadsList.set(pos + 1, this.downloadsList.get(pos));
			this.downloadsList.set(pos, temp);
		}
	}
	
	public void startDownload(int pos)
	{
		if (pos >= 0 && pos <= this.downloadsList.size())
		{
			if (!this.downloadsList.get(pos).isAlive())
			{
				this.downloadsList.get(pos).start();
			}
			else if (this.downloadsList.get(pos).isPaused())
			{
				this.downloadsList.get(pos).resumeDownload();
			}
		}
	}
	
	public void stopDownload(int pos)
	{
		if ((pos >= 0 && pos <= this.downloadsList.size())  && this.downloadsList.get(pos).isAlive())
		{
			this.downloadsList.get(pos).pauseDownload();
		}
	}
	
	public List<DownloadThread> getDownloadsList()
	{
		return this.downloadsList;
	}
}
