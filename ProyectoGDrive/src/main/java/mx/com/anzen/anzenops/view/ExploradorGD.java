package mx.com.anzen.anzenops.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import com.google.api.services.drive.model.File;

import mx.com.anzen.anzenops.control.APIGoogleDrive;

public class ExploradorGD extends JPanel implements TreeSelectionListener {

	private static final long serialVersionUID = -8951676851450604091L;
	private JPanel panelDetalles;
	private JTree tree;
	private URL helpURL;
	private static boolean DEBUG = false;

	// Optionally play with line styles. Possible values are
	// "Angled" (the default), "Horizontal", and "None".
	private static boolean playWithLineStyle = false;
	private static String lineStyle = "Horizontal";

	public ExploradorGD() {
		super(new GridLayout(1, 0));

		// Create the nodes.
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("Archivos en el Drive Anzen");

		// Create a tree that allows one selection at a time.
		tree = new JTree(top);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		// Listen for when the selection changes.
		tree.addTreeSelectionListener(this);

		if (playWithLineStyle) {
			System.out.println("line style = " + lineStyle);
			tree.putClientProperty("JTree.lineStyle", lineStyle);
		}

		// Create the scroll pane and add the tree to it.
		JScrollPane treeView = new JScrollPane(tree);

		// Create the HTML viewing pane.
		panelDetalles = new JPanel(new BorderLayout(3, 3));
		JScrollPane panelVista = new JScrollPane(panelDetalles);

		// Add the scroll panes to a split pane.
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setTopComponent(treeView);
		splitPane.setBottomComponent(panelVista);

		Dimension minimumSize = new Dimension(100, 50);
		panelVista.setMinimumSize(minimumSize);
		treeView.setMinimumSize(minimumSize);
		splitPane.setDividerLocation(100);
		splitPane.setPreferredSize(new Dimension(500, 300));

		// Add the split pane to this panel.
		add(splitPane);

		inicializaNodoRaiz(top);
	}

	/**
	 * Inicializa la vista
	 * 
	 * @param top
	 */
	private void inicializaNodoRaiz(DefaultMutableTreeNode top) {
		List<DefaultMutableTreeNode> lstNodosNivelAnterior = new ArrayList<>();
		List<DefaultMutableTreeNode> lstNodosNivelNuevo = null;
		lstNodosNivelAnterior.add(top);
		int contadorNivel = 0;

		// Generando Arbol
		try {

			do {
				lstNodosNivelNuevo = new ArrayList<>();

				for (DefaultMutableTreeNode nodoAnterior : lstNodosNivelAnterior) {
					top = nodoAnterior;

					List<File> lstCarpetas = null;
					if (contadorNivel == 0) {
						lstCarpetas = APIGoogleDrive.consultaCarpetas(null);
					} else {
						File carpeta = (File) nodoAnterior.getUserObject();
						lstCarpetas = APIGoogleDrive.consultaCarpetas(carpeta.getId());
					}

					if (lstCarpetas != null && lstCarpetas.size() != 0) {
						// Agregando nuevos nodos al nivel
						for (File carpeta : lstCarpetas) {
							DefaultMutableTreeNode nodo = new DefaultMutableTreeNode(carpeta);
							lstNodosNivelNuevo.add(nodo);
							top.add(nodo);
						}
					}
				}

				lstNodosNivelAnterior = lstNodosNivelNuevo;
				contadorNivel++;
			} while (lstNodosNivelNuevo != null && lstNodosNivelNuevo.size() != 0);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Implementacion de listener
	 */
	@Override
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

		if (node == null){
		    return;
		}

		List<File> archivos = null;
		if (node.getUserObject() instanceof File) {
			File folder = (File)node.getUserObject();
			try {
				archivos = APIGoogleDrive.consultaArchivos(folder.getId());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else {
			try {
				archivos = APIGoogleDrive.consultaArchivos(null);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		if(archivos != null && archivos.size()<0){
			for (File file : archivos) {
				panelDetalles.add(new Label(file.toString()));
			}
		}
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event dispatch thread.
	 */
	private static void createAndShowGUI() {

		// Create and set up the window.
		JFrame frame = new JFrame("Consulta Archivos Google Drive Anzen");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Add content to the window.
		frame.add(new ExploradorGD());

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * Inicializando la app
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Schedule a job for the event dispatch thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}
