package http.client;

import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * La classe DownloadTableModel repr�sente le mod�le utilis� par l'objet JTable �
 * l'int�rieur de AppFrame.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class DownloadTableModel extends AbstractTableModel
{
	// Titre des colonnes
	private String[] columnNames = {"Adresse Url", "Taille", "�tat"};
	
	// Liste des t�l�chargements
	private List<DownloadThread> downloadsList;
	
	/**
	 * Cr�er un nouveau mod�le pour le tableau.
	 * 
	 * @param data la liste � utiliser comme donn�es
	 */
	public DownloadTableModel(List<DownloadThread> data)
	{
		this.downloadsList = data;
	}
	
	/**
	 * Retourne le nombre de colonnes.
	 * 
	 * @return le nombre de colonnes
	 */
	public int getColumnCount()
	{
		return this.columnNames.length;
	}
	
	/**
	 * Retourne le nombre de rang�es.
	 * 
	 * @return le nombre de rang�es
	 */
	public int getRowCount()
	{
		return this.downloadsList.size();
	}
	
	/**
	 * Retourne la nom de la colonne pour l'indice de la colonne
	 * pass� en param�tre.
	 * 
	 * @param col l'indice de la colonne
	 * @return le nom de la colonne
	 */
	public String getColumnName(int col)
	{
		return this.columnNames[col];
	}
	
	/**
	 * Retourne la valeur de la case du tableau situ� � la rang�e et la colonne
	 * pass�s en param�tre.
	 * 
	 * @param row l'indice de la rang�e
	 * @param col l'indice de la colonne
	 * @return value la valeur de la case du tableau
	 */
	public Object getValueAt(int row, int col)
	{
		Object value = null;
		DownloadThread dl = this.downloadsList.get(row); 
		
		if (dl != null)
		{
			switch (col)
			{
			case 0:
				value = dl.getUrl();
				break;
			case 1:
				value = dl.getSize();
				break;
			case 2:
				value = dl.getCurrentState();
				break;
			}
		}
		
		return value;
	}
}
