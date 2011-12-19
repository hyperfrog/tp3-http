package http.client;

import http.client.DownloadThread.DownloadState;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * La classe AppDownload cr�er un gestionnaire de t�l�chargements qui s'occupe
 * de g�rer l'ordre des t�l�chargements, les threads associ�s aux t�l�chargements
 * et la liste des t�l�chargements.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class AppDownload
{
	// Nombre maximum de t�l�chargement simultan�s
	private static final int MAX_DOWNLOADS = 4;
	
	// Objet parent
	private AppFrame appFrame;
	
	// Liste des t�l�chargements
	private List<DownloadThread> downloadsList;
	// Liste des threads en cours
	private List<Thread> threadsList;
	// Nombre de t�l�chargements en cours
	private int nbCurrentDownloads;
	
	/**
	 * Cr�er un nouveau gestionnaire de t�l�chargement.
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
	 * Ajoute un t�l�chargement � la fin de la liste.
	 * 
	 * @param url l'adresse Url du fichier � t�l�charger
	 * @param savePath la destination o� sauvegarder le fichier
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
	 * Supprime le t�l�chargement de la liste � la position donn�e.
	 * La position doit �tre entre 0 et la taille de la liste.
	 * 
	 * @param pos la position du t�l�chargement � supprimer
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
	 * D�place le t�l�chargement � la position donn�e vers le haut dans la liste.
	 * La position doit �tre plus grande que 0 et plus petite que la taille 
	 * de la liste.
	 * 
	 * @param pos la position du t�l�chargement � d�placer
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
	 * D�place le t�l�chargement � la position donn�e vers le bas dans la liste.
	 * La position doit �tre plus grande ou �gale � 0 et plus petite que la 
	 * taille de la liste - 1.
	 * 
	 * @param pos la position du t�l�chargement � d�placer
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
	 * D�marre le t�l�chargement � la position donn�e.
	 * La position doit �tre entre 0 et la taille de la liste.
	 * 
	 * @param pos la position du t�l�chargement � d�marrer
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
	 * Arr�te le thread associ� au t�l�chargement dans la liste
	 * situ� � la position pass� en param�tre.
	 * La position doit �tre entre 0 et la taille de la liste.
	 * 
	 * @param pos la position du t�l�chargement � arr�ter
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
	 * Retourne la liste des t�l�chargements.
	 * 
	 * @return la liste des t�l�chargements
	 */
	public List<DownloadThread> getDownloadsList()
	{
		return this.downloadsList;
	}
	
	/**
	 * Met � jour la table des t�l�chargement, arr�te le t�l�chargement
	 * pass� en param�tre s'il est termin�. Dans ce cas, on d�marre le
	 * prochain t�l�chargement sur la liste d'attente.
	 * 
	 * @param dt le t�l�chargement � mettre � jour
	 */
	public void update(DownloadThread dt)
	{
		if (dt.isDone())
		{
			// Arr�te le thread du t�l�chargement
			this.stopDownload(this.downloadsList.indexOf(dt));
			
			// D�marre le prochain t�l�chargement en attente
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
		
		// Met � jour la liste des t�l�chargements
		this.appFrame.updateTable();
	}
}
