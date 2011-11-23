/**
 * author: Marcel Genzmehr
 * 19.08.2011
 */
package org.docear.plugin.core.workspace.node;

import java.awt.Component;
import java.io.File;
import java.net.URI;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.docear.plugin.core.DocearController;
import org.docear.plugin.core.event.DocearEvent;
import org.docear.plugin.core.event.DocearEventType;
import org.docear.plugin.core.workspace.IDocearMindmap;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.url.mindmapmode.MFileManager;
import org.freeplane.plugin.workspace.WorkspaceUtils;
import org.freeplane.plugin.workspace.controller.IWorkspaceNodeEventListener;
import org.freeplane.plugin.workspace.controller.WorkspaceNodeEvent;
import org.freeplane.plugin.workspace.io.annotation.ExportAsAttribute;
import org.freeplane.plugin.workspace.model.WorkspacePopupMenu;
import org.freeplane.plugin.workspace.model.WorkspacePopupMenuBuilder;
import org.freeplane.plugin.workspace.model.node.ALinkNode;
import org.freeplane.plugin.workspace.model.node.AWorkspaceTreeNode;

/**
 * 
 */
public class LinkTypeNewLiteratureNode extends ALinkNode implements IWorkspaceNodeEventListener, IDocearMindmap {
	private static final Icon DEFAULT_ICON = new ImageIcon(ResourceController.class.getResource("/images/docear16.png"));

	private static final long serialVersionUID = 1L;
	
	private URI linkPath;
	private WorkspacePopupMenu popupMenu = null;
	
	
	/***********************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************/

	public LinkTypeNewLiteratureNode(String type) {
		super(type);
	}


	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/
	
	@ExportAsAttribute("path")
	public URI getLinkPath() {
		return linkPath;
	}
	
	public void setLinkPath(URI linkPath) {
		this.linkPath = linkPath;
		if(this.linkPath != null) {
			DocearEvent event = new DocearEvent(this, DocearEventType.LIBRARY_NEW_MINDMAP_INDEXING_REQUEST, getLinkPath());
			DocearController.getController().dispatchDocearEvent(event);
		}
	}
	
	public boolean setIcons(DefaultTreeCellRenderer renderer) {
		renderer.setOpenIcon(DEFAULT_ICON);
		renderer.setClosedIcon(DEFAULT_ICON);
		renderer.setLeafIcon(DEFAULT_ICON);
		return true;
	}
	
	public void initializePopup() {
		if (popupMenu == null) {
						
			popupMenu  = new WorkspacePopupMenu();
			WorkspacePopupMenuBuilder.addActions(popupMenu, new String[] {
					"workspace.action.node.paste",
					"workspace.action.node.copy",
					"workspace.action.node.cut",
					WorkspacePopupMenuBuilder.SEPARATOR,
					"workspace.action.docear.node.rename"
			});
		}
		
	}
	
	private boolean createNewMindmap(final File f) {
		if (!createFolderStructure(f)) {
			return false;
		}

		MFileManager mFileManager = MFileManager.getController(Controller.getCurrentModeController());
		mFileManager.newMap();
		MapModel map = Controller.getCurrentController().getMap();
		map.getRootNode().setText(getName());
		DocearEvent evnt = new DocearEvent(this, DocearEventType.NEW_LITERATURE_MAP, map);
		DocearController.getController().dispatchDocearEvent(evnt);
		
		mFileManager.save(Controller.getCurrentController().getMap(), f);		
		Controller.getCurrentController().close(false);

		LogUtils.info("New Mindmap Created: " + f.getAbsolutePath());
		return true;
	}

	private boolean createFolderStructure(final File f) {
		final File folder = f.getParentFile();
		if (folder.exists()) {
			return true;
		}
		return folder.mkdirs();
	}
	
	protected AWorkspaceTreeNode clone(LinkTypeNewLiteratureNode node) {
		node.setLinkPath(getLinkPath());
		return super.clone(node);
	}
	
	/***********************************************************************************
	 * REQUIRED METHODS FOR INTERFACES
	 **********************************************************************************/
	
	public void handleEvent(WorkspaceNodeEvent event) {
		System.out.println("event: "+event.getType());
		if (event.getType() == WorkspaceNodeEvent.MOUSE_LEFT_DBLCLICK) {
			System.out.println("doublecklicked MindmapNode");
			try {				
				File f = WorkspaceUtils.resolveURI(getLinkPath());
				if(f == null) {
					return;
				}
				if (!f.exists()) {
					createNewMindmap(f);
				}
				Controller.getCurrentModeController().getMapController().newMap(f.toURL(), false);
				
			}
			catch (Exception e) {
				LogUtils.warn("could not open document (" + getLinkPath() + ")", e);
			}
		}
		else if (event.getType() == WorkspaceNodeEvent.MOUSE_RIGHT_CLICK) {			
				showPopup((Component) event.getBaggage(), event.getX(), event.getY());
		}
		else {
			// do nth for now
		}
	}

	public AWorkspaceTreeNode clone() {
		LinkTypeNewLiteratureNode node = new LinkTypeNewLiteratureNode(getType());
		return clone(node);
	}	
	
	public WorkspacePopupMenu getContextMenu() {
		if (popupMenu == null) {
			initializePopup();
		}
		return popupMenu;
	}
 
	public boolean changeNameTo(String newName) {
		// simple set the node name
		this.setName(newName);
		return true;
	}
}
