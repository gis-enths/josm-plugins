/***************************************************************************
 *   Copyright (C) 2009 by Tomasz Stelmach                                 *
 *   http://www.stelmach-online.net/                                       *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/

package org.openstreetmap.josm.plugins.piclayer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.piclayer.actions.SavePictureCalibrationAction;
import org.openstreetmap.josm.plugins.piclayer.actions.newlayer.NewLayerFromClipboardAction;
import org.openstreetmap.josm.plugins.piclayer.actions.newlayer.NewLayerFromFileAction;
import org.openstreetmap.josm.plugins.piclayer.actions.transform.MovePictureAction;
import org.openstreetmap.josm.plugins.piclayer.actions.transform.RotatePictureAction;
import org.openstreetmap.josm.plugins.piclayer.actions.transform.ScaleXPictureAction;
import org.openstreetmap.josm.plugins.piclayer.actions.transform.ScaleXYPictureAction;
import org.openstreetmap.josm.plugins.piclayer.actions.transform.ScaleYPictureAction;
import org.openstreetmap.josm.plugins.piclayer.actions.transform.ShearPictureAction;
import org.openstreetmap.josm.plugins.piclayer.actions.transform.affine.MovePointAction;
import org.openstreetmap.josm.plugins.piclayer.actions.transform.affine.RemovePointAction;
import org.openstreetmap.josm.plugins.piclayer.actions.transform.affine.TransformPointAction;
import org.openstreetmap.josm.plugins.piclayer.layer.PicLayerAbstract;

/**
 * Main Plugin class.
 */
public class PicLayerPlugin extends Plugin implements LayerChangeListener, ActiveLayerChangeListener {

    public static List<IconToggleButton> buttonList = null;

    // Plugin menu
    JosmAction newLayerFromFileAction = new NewLayerFromFileAction();
    JosmAction newLayerFromClipboardAction = new NewLayerFromClipboardAction();
    /**
     * Constructor...
     */
    public PicLayerPlugin(PluginInformation info) {
        super(info);

        // Create menu entry

        // Add menu items
        MainMenu.add(Main.main.menu.imagerySubMenu, newLayerFromFileAction);
        MainMenu.add(Main.main.menu.imagerySubMenu, newLayerFromClipboardAction);
        updateEnabledState();
        // Listen to layers
        Main.getLayerManager().addLayerChangeListener(this);
        Main.getLayerManager().addActiveLayerChangeListener(this);
    }

    /**
     * Called when the map is created. Creates the toolbar buttons.
     */
    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (newFrame != null) {
            // Create plugin map modes
            MovePictureAction movePictureAction = new MovePictureAction(newFrame);
            MovePointAction movePointAction = new MovePointAction(newFrame);
            TransformPointAction transformPointAction = new TransformPointAction(newFrame);
            RemovePointAction removePointAction = new RemovePointAction(newFrame);

            RotatePictureAction rotatePictureAction = new RotatePictureAction(newFrame);
            ScaleXYPictureAction scaleXYPictureAction = new ScaleXYPictureAction(newFrame);
            ScaleXPictureAction scaleXPictureAction = new ScaleXPictureAction(newFrame);
            ScaleYPictureAction scaleYPictureAction = new ScaleYPictureAction(newFrame);
            ShearPictureAction shearPictureAction = new ShearPictureAction(newFrame);
            // Create plugin buttons and add them to the toolbar

            buttonList = new ArrayList<>(7);
            buttonList.add(picLayerActionButtonFactory(movePictureAction));
            buttonList.add(picLayerActionButtonFactory(movePointAction));
            buttonList.add(picLayerActionButtonFactory(transformPointAction));
            buttonList.add(picLayerActionButtonFactory(removePointAction));
            buttonList.add(picLayerActionButtonFactory(rotatePictureAction));
            buttonList.add(picLayerActionButtonFactory(scaleXYPictureAction));
            buttonList.add(picLayerActionButtonFactory(scaleXPictureAction));
            buttonList.add(picLayerActionButtonFactory(scaleYPictureAction));
            buttonList.add(picLayerActionButtonFactory(shearPictureAction));

            for (IconToggleButton btn : buttonList) {
                newFrame.addMapMode(btn);
            }
        }
    }

    private IconToggleButton picLayerActionButtonFactory(MapMode action) {
        IconToggleButton button = new IconToggleButton(action);
        button.setAutoHideDisabledButton(true);
        return button;
    }

    /**
     * The toolbar buttons shall be active and visible only when the PicLayer is active.
     */
    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        Layer oldLayer = e.getPreviousActiveLayer();
        Layer newLayer = Main.getLayerManager().getActiveLayer();
        boolean oldPic = oldLayer instanceof PicLayerAbstract;
        boolean newPic = newLayer instanceof PicLayerAbstract;

        if (oldPic) {
            ((PicLayerAbstract) oldLayer).setDrawPoints(false);
        }

        if (newPic) {
            ((PicLayerAbstract) newLayer).setDrawPoints(true);
        }
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // Do nothing
    }

    /**
     * The menu is enabled once another layer is first created. This is needed
     * because the picture must be positioned based on the current mapview (so
     * one must exist first). User should not be able to load a picture too early.
     */
    @Override
    public void layerAdded(LayerAddEvent e) {
        newLayerFromFileAction.setEnabled(true);
        newLayerFromClipboardAction.setEnabled(true);
    }

    /**
     * When all layers are gone - the menu is gone too.
     */
    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        if (e.getRemovedLayer() instanceof PicLayerAbstract && ((PicLayerAbstract) e.getRemovedLayer()).getTransformer().isModified()) {
            if (JOptionPane.showConfirmDialog(Main.parent, tr("Do you want to save current calibration of layer {0}?",
                    ((PicLayerAbstract) e.getRemovedLayer()).getPicLayerName()),
                    tr("Select an option"),
                    JOptionPane.YES_NO_OPTION) == 0)
                new SavePictureCalibrationAction((PicLayerAbstract) e.getRemovedLayer()).actionPerformed(null);
        }
        updateEnabledState();
    }

    private void updateEnabledState() {
        boolean enable = !Main.getLayerManager().getLayers().isEmpty();
        newLayerFromFileAction.setEnabled(enable);
        newLayerFromClipboardAction.setEnabled(enable);
    }
}
