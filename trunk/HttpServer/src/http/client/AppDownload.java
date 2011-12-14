package http.client;

import http.client.DownloadThread.DownloadState;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * La classe AppDownload
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class AppDownload
{
	// Nombre maximum de téléchargement simultanés
	private static final int MAX_DOWNLOADS = 4;
	
	// 
	private AppFrame appFrame;
	
	// 
	private List<DownloadThread> downloadsList;
	// 
	private int nbCurrentDownloads;
	
	/**
	 * 
	 * @param appFrame
	 */
	public AppDownload(AppFrame appFrame)
	{
		this.appFrame = appFrame;
		
		this.downloadsList = new ArrayList<DownloadThread>();
		this.nbCurrentDownloads = 0;
	}
	
	/**
	 * 
	 * @param url
	 * @param savePath
	 */
	public void addDownload(String url, String savePath)
	{
		URL newUrl = null;
		try
		{
			newUrl = new URL(url);
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		
		DownloadThread dl = new DownloadThread(this, newUrl, savePath);
		this.downloadsList.add(dl);
	}
	
	/**
	 * 
	 * @param pos
	 */
	public void removeDownload(int pos)
	{
		if (this.downloadsList.size() > 0 && (pos >= 0 && pos < this.downloadsList.size()))
		{
			this.stopDownload(pos);
			this.downloadsList.remove(pos);
		}
	}
	
	/**
	 * 
	 * @param pos
	 */
	public void moveUp(int pos)
	{
		if (this.downloadsList.size() > 1 && (pos > 0 && pos < this.downloadsList.size()))
		{
			DownloadThread temp = this.downloadsList.get(pos - 1);
			this.downloadsList.set(pos - 1, this.downloadsList.get(pos));
			this.downloadsList.set(pos, temp);
		}
	}
	
	/**
	 * 
	 * @param pos
	 */
	public void moveDown(int pos)
	{
		if (this.downloadsList.size() > 1 && (pos >= 0 && pos < this.downloadsList.size() - 1))
		{
			DownloadThread temp = this.downloadsList.get(pos + 1);
			this.downloadsList.set(pos + 1, this.downloadsList.get(pos));
			this.downloadsList.set(pos, temp);
		}
	}
	
	/**
	 * 
	 * @param pos
	 */
	public void startDownload(int pos)
	{
		if ((pos >= 0 && pos < this.downloadsList.size()) 
				&& !this.downloadsList.get(pos).isAlive() 
				&& !this.downloadsList.get(pos).isDone())
		{
			if (this.nbCurrentDownloads < AppDownload.MAX_DOWNLOADS)
			{
				this.downloadsList.get(pos).start();
				this.nbCurrentDownloads = Math.min(this.downloadsList.size(), this.nbCurrentDownloads + 1);
			}
			else
			{
				this.downloadsList.get(pos).setCurrentState(DownloadState.WAITING);
			}
		}
	}
	
	/**
	 * 
	 * @param pos
	 */
	public void stopDownload(int pos)
	{
		if ((pos >= 0 && pos < this.downloadsList.size()) 
				&& !this.downloadsList.get(pos).isInterrupted() 
				&& !this.downloadsList.get(pos).isDone())
		{
			this.stopDownload(this.downloadsList.get(pos));
		}
	}
	
	//
	private void stopDownload(DownloadThread dt)
	{
		dt.interrupt();
		this.nbCurrentDownloads = Math.max(0, this.nbCurrentDownloads - 1);
	}
	
	/**
	 * 
	 * @return
	 */
	public List<DownloadThread> getDownloadsList()
	{
		return this.downloadsList;
	}
	
	/**
	 * 
	 * @param dt
	 */
	public void update(DownloadThread dt)
	{
		if (dt.isDone())
		{
			this.stopDownload(dt);
			
			if (this.nbCurrentDownloads < AppDownload.MAX_DOWNLOADS)
			{
				for (int i = 0; i < this.downloadsList.size(); i++)
				{
					if (this.downloadsList.get(i).getCurrentState().equals(DownloadState.WAITING))
					{
						this.startDownload(i);
						break;
					}
				}
			}
		}
		
		this.appFrame.updateTable();
	}
}
