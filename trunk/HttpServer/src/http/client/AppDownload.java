package http.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AppDownload
{
	private static final int MAX_DOWNLOADS = 4;
	
	private List<DownloadThread> downloadsList;
	private int nbCurrentDownloads;
	
	public AppDownload()
	{
		this.downloadsList = new ArrayList<DownloadThread>();
		this.nbCurrentDownloads = 0;
	}
	
	public void addDownload(String url, String savePath) throws MalformedURLException
	{
		URL newUrl = new URL(url);
			
		DownloadThread dl = new DownloadThread(newUrl, savePath);
		this.downloadsList.add(dl);
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
			
			this.nbCurrentDownloads--;
		}
	}
	
	public void moveUp(int pos)
	{
		if (this.downloadsList.size() > 1 && (pos > 0 && pos <= this.downloadsList.size()))
		{
			DownloadThread temp = this.downloadsList.get(pos - 1);
			this.downloadsList.set(pos - 1, this.downloadsList.get(pos));
			this.downloadsList.set(pos, temp);
		}
	}
	
	public void moveDown(int pos)
	{
		if (this.downloadsList.size() > 1 && (pos >= 0 && pos < this.downloadsList.size()))
		{
			DownloadThread temp = this.downloadsList.get(pos + 1);
			this.downloadsList.set(pos + 1, this.downloadsList.get(pos));
			this.downloadsList.set(pos, temp);
		}
	}
	
	public void startDownload(int pos)
	{
		if (this.nbCurrentDownloads < AppDownload.MAX_DOWNLOADS && (pos >= 0 && pos <= this.downloadsList.size()) && !this.downloadsList.get(pos).isAlive())
		{	
			this.downloadsList.get(pos).start();
			this.nbCurrentDownloads++;
		}
	}
	
	public void stopDownload(int pos)
	{
		if ((pos >= 0 && pos <= this.downloadsList.size())  && this.downloadsList.get(pos).isAlive())
		{
			//this.downloadsList.get(pos).closeConnection();
			this.removeDownload(pos);
		}
	}
	
	public List<DownloadThread> getDownloadsList()
	{
		return this.downloadsList;
	}
}
