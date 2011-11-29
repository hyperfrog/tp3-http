package http.client.model;

import java.util.List;
import javax.swing.table.AbstractTableModel;

import http.client.DownloadThread;

public class DownloadTableModel extends AbstractTableModel
{
	private String[] columnNames = {"URL", "Progression"};
	private List<DownloadThread> downloadsList;
	
	public DownloadTableModel(List<DownloadThread> data)
	{
		this.downloadsList = data;
	}
	
	public int getColumnCount()
	{
		return this.columnNames.length;
	}
	
	public int getRowCount()
	{
		return this.downloadsList.size();
	}
	
	public String getColumnName(int col)
	{
		return this.columnNames[col];
	}
	
	public Object getValueAt(int row, int col)
	{
		Object value = null;
		DownloadThread dl = this.downloadsList.get(row); 
		
		if (dl != null)
		{
			switch (col)
			{
			case 0:
				value = dl.getPath();
				break;
			case 1:
				value = dl.isDone();
				break;
			}
		}
		
		return value;
	}
	
	public void update()
	{
		// TODO : Mettre à jour seulement la rangée
		this.fireTableDataChanged();
	}
}
