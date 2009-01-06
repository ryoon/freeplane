/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.mindmapmode;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.freeplane.core.controller.Controller;
import org.freeplane.core.modecontroller.MapController;
import org.freeplane.core.modecontroller.ModeController;
import org.freeplane.core.model.NodeModel;
import org.freeplane.core.ui.FreeplaneAction;
import org.freeplane.core.undo.IUndoableActor;
import org.freeplane.features.common.text.TextController;
import org.freeplane.features.mindmapmode.text.MTextController;
import org.freeplane.view.swing.map.NodeView;

class NewChildAction extends FreeplaneAction {
	public NewChildAction() {
		super("new_child", "/images/idea.png");
	}

	public void actionPerformed(final ActionEvent e) {
		((MMapController) getModeController().getMapController()).addNewNode(
		    MMapController.NEW_CHILD, null);
	}

	public NodeModel addNewNode(int newNodeMode, final KeyEvent e) {
		final ModeController modeController = getModeController();
		final NodeModel target = modeController.getMapController().getSelectedNode();
		((MTextController) TextController.getController(modeController)).stopEditing();
		final NodeModel targetNode = target;
		NodeModel newNode = null;
		final NodeView targetNodeView = Controller.getController().getMapView().getNodeView(target);
		switch (newNodeMode) {
			case MMapController.NEW_SIBLING_BEFORE:
			case MMapController.NEW_SIBLING_BEHIND: {
				if (!targetNode.isRoot()) {
					final NodeModel parent = targetNode.getParentNode();
					int childPosition = parent.getChildPosition(targetNode);
					if (newNodeMode == MMapController.NEW_SIBLING_BEHIND) {
						childPosition++;
					}
					newNode = addNewNode(parent, childPosition, targetNode.isLeft());
					final NodeView nodeView = Controller.getController().getMapView().getNodeView(
					    newNode);
					modeController.getMapController().select(newNode);
					((MTextController) TextController.getController(modeController)).edit(nodeView,
					    targetNodeView, e, true, false, false);
					break;
				}
				else {
					newNodeMode = MMapController.NEW_CHILD;
				}
			}
			case MMapController.NEW_CHILD:
			case MMapController.NEW_CHILD_WITHOUT_FOCUS: {
				final MapController mapController = modeController.getMapController();
				final boolean parentFolded = mapController.isFolded(targetNode);
				if (parentFolded) {
					mapController.setFolded(targetNode, false);
				}
				final int position = Controller.getResourceController().getProperty(
				    "placenewbranches").equals("last") ? targetNode.getChildCount() : 0;
				newNode = addNewNode(targetNode, position, targetNode.isNewChildLeft());
				final NodeView nodeView = Controller.getController().getMapView().getNodeView(
				    newNode);
				if (newNodeMode == MMapController.NEW_CHILD) {
					modeController.getMapController().select(nodeView.getModel());
				}
				((MTextController) TextController.getController(modeController)).edit(nodeView,
				    targetNodeView, e, true, parentFolded, false);
				break;
			}
		}
		return newNode;
	}

	public NodeModel addNewNode(final NodeModel parent, final int index, final boolean newNodeIsLeft) {
		final NodeModel newNode = getModeController().getMapController().newNode("",
		    parent.getMap());
		newNode.setLeft(newNodeIsLeft);
		final IUndoableActor actor = new IUndoableActor() {
			public void act() {
				(getModeController().getMapController()).insertNodeIntoWithoutUndo(newNode, parent,
				    index);
			}

			public String getDescription() {
				return "paste";
			}

			public void undo() {
				((MMapController) getModeController().getMapController())
				    .deleteWithoutUndo(newNode);
			}
		};
		getModeController().execute(actor);
		return newNode;
	}
}
