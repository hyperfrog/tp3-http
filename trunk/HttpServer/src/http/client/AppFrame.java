package http.client;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;


public class AppFrame extends JFrame implements ActionListener
{
	private static final String TITLE = "HttpClient";
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	
	// 
	private AppDownload appDownload;
	// 
	private JPanel buttonsPanel;
	// 
	private JButton addButton;
	// 
	private JButton removeButton;
	// 
	private JButton moveUpButton;
	// 
	private JButton moveDownButton;
	// 
	private JButton stopButton;
	// 
	private JButton startButton;
	// 
	private JTable downloadsTable;
	
	public AppFrame()
	{
		super();
		
		this.appDownload = new AppDownload();
		
		this.buttonsPanel = new JPanel();
		this.addButton = new JButton();
		this.removeButton = new JButton();
		this.moveUpButton = new JButton();
		this.moveDownButton = new JButton();
		this.stopButton = new JButton();
		this.startButton = new JButton();
		this.downloadsTable = new JTable();
		
		this.setTitle(AppFrame.TITLE);
		this.setSize(AppFrame.WIDTH, AppFrame.HEIGHT);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		this.getContentPane().setLayout(new BorderLayout(10, 10));
		
		this.buttonsPanel.setLayout(new GridLayout(6, 0, 10, 10));
		
		this.addButton.setText("Ajouter");
		this.addButton.setActionCommand("ADD");
		
		this.removeButton.setText("Supprimer");
		this.removeButton.setActionCommand("DELETE");
		
		this.moveUpButton.setText("Monter"); 
		this.moveUpButton.setActionCommand("MOVE_UP");
		
		this.moveDownButton.setText("Descendre");
		this.moveDownButton.setActionCommand("MOVE_DOWN");
		
		this.stopButton.setText("Arrêter");
		this.stopButton.setActionCommand("STOP");
		
		this.startButton.setText("Démarrer");
		this.startButton.setActionCommand("START");
		
		this.buttonsPanel.add(this.addButton);
		this.buttonsPanel.add(this.removeButton);
		this.buttonsPanel.add(this.moveUpButton);
		this.buttonsPanel.add(this.moveDownButton);
		this.buttonsPanel.add(this.stopButton);
		this.buttonsPanel.add(this.startButton);
		
		DownloadTableModel tableModel = new DownloadTableModel(this.appDownload.getDownloadsList());
		this.downloadsTable.setModel(tableModel);
		this.downloadsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JScrollPane scrollpane = new JScrollPane(this.downloadsTable);
		
		this.getContentPane().add(scrollpane, BorderLayout.CENTER);
		this.getContentPane().add(this.buttonsPanel, BorderLayout.EAST);
		
		this.addButton.addActionListener(this);
		this.removeButton.addActionListener(this);
		this.moveUpButton.addActionListener(this);
		this.moveDownButton.addActionListener(this);
		this.stopButton.addActionListener(this);
		this.startButton.addActionListener(this);
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
			
			if (newUrl != null && newUrl.length() != 0)
			{
				this.appDownload.addDownload(newUrl);
				needUpdate = true;
			}
		}
		else if (evt.getActionCommand().equals("DELETE"))
		{
			if (this.downloadsTable.getSelectedRow() != -1)
			{
				// TODO : Confirmation
				this.appDownload.removeDownload(this.downloadsTable.getSelectedRow());
				needUpdate = true;
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
			((DownloadTableModel) this.downloadsTable.getModel()).update();
		}
	}
}
