package http.client;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

public class AppToolBar extends JToolBar
{
	// Images utilis�es par la classe pour les boutons
	private static BufferedImage addImage = null;
	private static BufferedImage removeImage = null;
	private static BufferedImage moveUpImage = null;
	private static BufferedImage moveDownImage = null;
	private static BufferedImage startImage = null;
	private static BufferedImage stopImage = null;
	
	// Initialisation des images
    static
    {
    	try
    	{
    		AppToolBar.addImage = ImageIO.read(AppToolBar.class.getResource("./res/add.png"));
    		AppToolBar.removeImage = ImageIO.read(AppToolBar.class.getResource("./res/remove.png"));
    		AppToolBar.moveUpImage = ImageIO.read(AppToolBar.class.getResource("./res/move_up.png"));
    		AppToolBar.moveDownImage = ImageIO.read(AppToolBar.class.getResource("./res/move_down.png"));
    		AppToolBar.startImage = ImageIO.read(AppToolBar.class.getResource("./res/start.png"));
    		AppToolBar.stopImage = ImageIO.read(AppToolBar.class.getResource("./res/stop.png"));
    	}
    	catch (IOException e)
    	{
    		System.out.println(e.getMessage());
    	}
    	catch (IllegalArgumentException e)
    	{
    		System.out.println("Incapable de trouver une ou plusieurs image(s) de la classe AppToolBar.");
    	}
    }
	
	private AppFrame parent;
	private JButton addButton;
	private JButton removeButton;
	private JButton moveUpButton;
	private JButton moveDownButton;
	private JButton stopButton;
	private JButton startButton;
	
	public AppToolBar(AppFrame parent)
	{
		super();
		
		this.parent = parent;
		
		this.setFloatable(false);
		this.initComponents();
	}
	
	public void initComponents()
	{
		this.addButton = new JButton();
		this.removeButton = new JButton();
		this.moveUpButton = new JButton();
		this.moveDownButton = new JButton();
		this.stopButton = new JButton();
		this.startButton = new JButton();
		
		this.addButton.setText(null);
		this.addButton.setToolTipText("Ajouter");
		this.addButton.setActionCommand("ADD");
		this.addButton.setIcon(AppToolBar.addImage != null ? new ImageIcon(AppToolBar.addImage) : null);
		
		this.removeButton.setText(null);
		this.removeButton.setToolTipText("Enlever");
		this.removeButton.setActionCommand("DELETE");
		this.removeButton.setIcon(AppToolBar.removeImage != null ? new ImageIcon(AppToolBar.removeImage) : null);
		
		this.moveUpButton.setText(null);
		this.moveUpButton.setToolTipText("Monter");
		this.moveUpButton.setActionCommand("MOVE_UP");
		this.moveUpButton.setIcon(AppToolBar.moveUpImage != null ? new ImageIcon(AppToolBar.moveUpImage) : null);
		
		this.moveDownButton.setText(null);
		this.moveDownButton.setToolTipText("Descendre");
		this.moveDownButton.setActionCommand("MOVE_DOWN");
		this.moveDownButton.setIcon(AppToolBar.moveDownImage != null ? new ImageIcon(AppToolBar.moveDownImage) : null);
		
		this.stopButton.setText(null);
		this.stopButton.setToolTipText("Arr�ter");
		this.stopButton.setActionCommand("STOP");
		this.stopButton.setIcon(AppToolBar.stopImage != null ? new ImageIcon(AppToolBar.stopImage) : null);
		
		this.startButton.setText(null);
		this.startButton.setToolTipText("D�marrer");
		this.startButton.setActionCommand("START");
		this.startButton.setIcon(AppToolBar.startImage != null ? new ImageIcon(AppToolBar.startImage) : null);
		
		this.add(this.addButton);
		this.add(this.removeButton);
		this.add(this.startButton);
		this.add(this.stopButton);
		this.addSeparator();
		this.add(this.moveUpButton);
		this.add(this.moveDownButton);
		
		this.addButton.addActionListener(this.parent);
		this.removeButton.addActionListener(this.parent);
		this.moveUpButton.addActionListener(this.parent);
		this.moveDownButton.addActionListener(this.parent);
		this.stopButton.addActionListener(this.parent);
		this.startButton.addActionListener(this.parent);
	}
}
