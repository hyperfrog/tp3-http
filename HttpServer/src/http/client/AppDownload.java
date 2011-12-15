package http.client;

import http.client.DownloadThread.DownloadState;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * La classe AppDownload créer un gestionnaire de téléchargements qui s'occupe
 * de gérer l'ordre des téléchargements, les threads associés aux téléchargements
 * et la liste des téléchargements.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class AppDownload
{
	// Nombre maximum de téléchargement simultanés
	private static final int MAX_DOWNLOADS = 4;
	
	// Objet parent
	private AppFrame appFrame;
	
	// Liste des téléchargements
	private List<DownloadThread> downloadsList;
	// Liste des threads en cours
	private List<Thread> threadsList;
	// Nombre de téléchargements en cours
	private int nbCurrentDownloads;
	
	/**
	 * Créer un nouveau gestionnaire de téléchargement.
	 * 
	 * @param appFrame l'objet AppFrame parent
	 */
	public AppDownload(AppFrame appFrame)
	{
		this.appFrame = appFrame;
		
		this.downloadsList = new ArrayList<DownloadThread>();
		this.threadsList = new ArrayList<Thread>();
		this.nbCurrentDownloads = 0;
	}
	
	/**
	 * Ajoute un téléchargement à la fin de la liste.
	 * 
	 * @param url l'adresse Url du fichier à télécharger
	 * @param savePath la destination où sauvegarder le fichier
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
		this.threadsList.add(null);
	}
	
	/**
	 * Supprime le téléchargement de la liste à la position donnée.
	 * La position doit être entre 0 et la taille de la liste.
	 * 
	 * @param pos la position du téléchargement à supprimer
	 */
	public void removeDownload(int pos)
	{
		if (this.downloadsList.size() > 0 && (pos >= 0 && pos < this.downloadsList.size()))
		{
			this.stopDownload(pos);
			
			this.downloadsList.remove(pos);
			this.threadsList.remove(pos);
		}
	}
	
	/**
	 * Déplace le téléchargement à la position donnée vers le haut dans la liste.
	 * La position doit être plus grande que 0 et plus petite que la taille 
	 * de la liste.
	 * 
	 * @param pos la position du téléchargement à déplacer
	 */
	public void moveUp(int pos)
	{
		if (this.downloadsList.size() > 1 && (pos > 0 && pos < this.downloadsList.size()))
		{
			DownloadThread tempDownload = this.downloadsList.get(pos - 1);
			this.downloadsList.set(pos - 1, this.downloadsList.get(pos));
			this.downloadsList.set(pos, tempDownload);
			
			Thread tempThread = this.threadsList.get(pos - 1);
			this.threadsList.set(pos - 1, this.threadsList.get(pos));
			this.threadsList.set(pos, tempThread);
		}
	}
	
	/**
	 * Déplace le téléchargement à la position donnée vers le bas dans la liste.
	 * La position doit être plus grande ou égale à 0 et plus petite que la 
	 * taille de la liste - 1.
	 * 
	 * @param pos la position du téléchargement à déplacer
	 */
	public void moveDown(int pos)
	{
		if (this.downloadsList.size() > 1 && (pos >= 0 && pos < this.downloadsList.size() - 1))
		{
			DownloadThread tempDownload = this.downloadsList.get(pos + 1);
			this.downloadsList.set(pos + 1, this.downloadsList.get(pos));
			this.downloadsList.set(pos, tempDownload);
			
			Thread tempThread = this.threadsList.get(pos + 1);
			this.threadsList.set(pos + 1, this.threadsList.get(pos));
			this.threadsList.set(pos, tempThread);
		}
	}
	
	/**
	 * Démarre le téléchargement à la position donnée.
	 * La position doit être entre 0 et la taille de la liste.
	 * 
	 * @param pos la position du téléchargement à démarrer
	 */
	public void startDownload(int pos)
	{
		if ((pos >= 0 && pos < this.downloadsList.size()) 
				&& !this.downloadsList.get(pos).isDone()
				&& this.threadsList.get(pos) == null)
		{
			if (this.nbCurrentDownloads < AppDownload.MAX_DOWNLOADS)
			{
				Thread t = new Thread(this.downloadsList.get(pos));
				t.setDaemon(true);
				t.start();
				
				this.threadsList.set(pos, t);
				
				this.nbCurrentDownloads = Math.min(this.downloadsList.size(), this.nbCurrentDownloads + 1);
			}
			else
			{
				this.downloadsList.get(pos).setCurrentState(DownloadState.WAITING);
			}
		}
	}
	
	/**
	 * Arrête le thread associé au téléchargement dans la liste
	 * situé à la position passé en paramètre.
	 * La position doit être entre 0 et la taille de la liste.
	 * 
	 * @param pos la position du téléchargement à arrêter
	 */
	public void stopDownload(int pos)
	{
		if ((pos >= 0 && pos < this.downloadsList.size()) 
				&& !this.downloadsList.get(pos).isDone()
				&& this.threadsList.get(pos) != null)
		{
			this.downloadsList.get(pos).setCurrentState(DownloadState.STOPPED);
			this.threadsList.set(pos, null);
			
			this.nbCurrentDownloads = Math.max(0, this.nbCurrentDownloads - 1);
		}
	}
	
	/**
	 * Retourne la liste des téléchargements.
	 * 
	 * @return la liste des téléchargements
	 */
	public List<DownloadThread> getDownloadsList()
	{
		return this.downloadsList;
	}
	
	/**
	 * Met à jour la table des téléchargement, arrête le téléchargement
	 * passé en paramètre s'il est terminé. Dans ce cas, on démarre le
	 * prochain téléchargement sur la liste d'attente.
	 * 
	 * @param dt le téléchargement à mettre à jour
	 */
	public void update(DownloadThread dt)
	{
		if (dt.isDone())
		{
			// Arrête le thread du téléchargement
			this.stopDownload(this.downloadsList.indexOf(dt));
			
			// Démarre le prochain téléchargement en attente
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
		
		// Met à jour la liste des téléchargements
		this.appFrame.updateTable();
	}
}
