package http.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.net.MalformedURLException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

public class AppFrame extends JFrame implements ActionListener, ComponentListener
{
	private static final Dimension INIT_SIZE = new Dimension(600, 300);
	private static final int MIN_WIDTH = 600;
	private static final int MIN_HEIGHT = 300;
	private static final String INIT_TITLE = "Outils de téléchargement";
	
	private AppDownload appDownload;
	private AppToolBar appToolBar; 
	private JTable downloadsTable;
	
	public AppFrame()
	{
		super();
		this.setNativeLookAndFeel();
		
		this.appDownload = new AppDownload();
		this.appToolBar = new AppToolBar(this);
		this.downloadsTable = new JTable();
		
		this.setTitle(AppFrame.INIT_TITLE);
		this.setSize(AppFrame.INIT_SIZE);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		DownloadTableModel tableModel = new DownloadTableModel(this.appDownload.getDownloadsList());
		this.downloadsTable.setModel(tableModel);
		this.downloadsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JScrollPane scrollpane = new JScrollPane(this.downloadsTable);
		
		this.getContentPane().setLayout(new BorderLayout());
		
		this.getContentPane().add(this.appToolBar, BorderLayout.PAGE_START);
		this.getContentPane().add(scrollpane, BorderLayout.CENTER);
		
		this.addComponentListener(this);
	}
	
	private void setNativeLookAndFeel()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			System.err.println("Incapable de changer l'apparence de l'application.");
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent evt)
	{
		boolean needUpdate = false;
		
		if (evt.getActionCommand().equals("ADD"))
		{
			String newUrl = JOptionPane.showInputDialog(
					this, 
					"Entrer le lien à télécharger : ", 
					"Ajout d'un téléchargement", 
					JOptionPane.DEFAULT_OPTION);
			
			if (newUrl != null && newUrl.length() > 0)
			{
				// TODO : Verif destination
				String savePath = JOptionPane.showInputDialog(
						this, 
						"Entrer la destination du fichier : ", 
						"Destination", 
						JOptionPane.DEFAULT_OPTION);
				
				if (savePath != null && savePath.length() > 0)
				{
					try
					{
						this.appDownload.addDownload(newUrl, savePath);
					}
					catch (MalformedURLException e)
					{
						JOptionPane.showMessageDialog(this, e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
						e.printStackTrace();
					}
					
					needUpdate = true;
				}
				else
				{
					JOptionPane.showMessageDialog(this, "Il y une erreur avec la destination entrée.", "Erreur", JOptionPane.ERROR_MESSAGE);
				}
			}
			else
			{
				JOptionPane.showMessageDialog(this, "Il y a une erreur avec l'adresse entrée.", "Erreur", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if (evt.getActionCommand().equals("DELETE"))
		{
			if (this.downloadsTable.getSelectedRow() != -1)
			{
				int response = JOptionPane.showConfirmDialog(
						this, 
						"Êtes-vous sûr de vouloir supprimer ce téléchargement ?",
						"Confirmation", 
						JOptionPane.YES_NO_OPTION, 
						JOptionPane.QUESTION_MESSAGE);
				
				if (response == JOptionPane.YES_OPTION)
				{
					this.appDownload.removeDownload(this.downloadsTable.getSelectedRow());
					needUpdate = true;
				}
			}
		}
		else if (evt.getActionCommand().equals("MOVE_UP"))
		{
			if (this.downloadsTable.getSelectedRow() != -1)
			{
				this.appDownload.moveUp(this.downloadsTable.getSelectedRow());
				needUpdate = true;
			}
		}
		else if (evt.getActionCommand().equals("MOVE_DOWN"))
		{
			if (this.downloadsTable.getSelectedRow() != -1)
			{
				this.appDownload.moveDown(this.downloadsTable.getSelectedRow());
				needUpdate = true;
			}
		}
		else if (evt.getActionCommand().equals("START"))
		{
			if (this.downloadsTable.getSelectedRow() != -1)
			{
				this.appDownload.startDownload(this.downloadsTable.getSelectedRow());
			}
		}
		else if (evt.getActionCommand().equals("STOP"))
		{
			if (this.downloadsTable.getSelectedRow() != -1)
			{
				this.appDownload.stopDownload(this.downloadsTable.getSelectedRow());
			}
		}
		
		if (needUpdate)
		{
			((DownloadTableModel) this.downloadsTable.getModel()).fireTableDataChanged();
		}
	}
	
	@Override
	public void componentResized(ComponentEvent evt)
	{
		int width = getWidth();
		int height = getHeight();
		// Vérifie si la largeur et la hauteur sont inférieures 
		// à la valeur minimale permise pour chacune
		boolean resize = false;
		if (width < AppFrame.MIN_WIDTH)
		{
			resize = true;
			width = AppFrame.MIN_WIDTH;
		}
		if (height < AppFrame.MIN_HEIGHT)
		{
			resize = true;
			height = AppFrame.MIN_HEIGHT;
		}
		if (resize)
		{
			this.setSize(width, height);
		}
	}
	
	@Override
	public void componentHidden(ComponentEvent evt)
	{	
	}

	@Override
	public void componentMoved(ComponentEvent evt)
	{
	}
	
	@Override
	public void componentShown(ComponentEvent evt)
	{
	}
}
