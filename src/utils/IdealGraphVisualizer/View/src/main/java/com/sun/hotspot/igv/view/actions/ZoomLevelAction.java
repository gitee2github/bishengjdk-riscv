/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */
package com.sun.hotspot.igv.view.actions;

import com.sun.hotspot.igv.data.ChangedListener;
import com.sun.hotspot.igv.view.DiagramViewer;
import com.sun.hotspot.igv.view.EditorTopComponent;
import java.awt.Color;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;

public final class ZoomLevelAction extends JComboBox<String> implements ChangedListener<DiagramViewer> {

    private static String[] choices = { "25%", "50%", "75%", "100%", "125%", "150%","200%","300%"};

    private DiagramViewer diagramScene;

    @Override
    public void actionPerformed(ActionEvent e) {
        EditorTopComponent editor = EditorTopComponent.getActive();
        if (editor != null) {
            editor.requestActive();
        }

        String levelStr = (String)this.getSelectedItem();
        levelStr = levelStr.replaceAll("\\s","");
        levelStr = levelStr.replaceFirst("%","");
        try{
            int level = Integer.parseInt(levelStr);
            if (level > 0 && level < 1000) {
                this.setZoomLevel(level);
            } else {
                this.setZoomLevel(100);
            }
        } catch(NumberFormatException exception){
            this.setZoomLevel(100);
        }
    }

    public ZoomLevelAction(DiagramViewer scene) {
        super();
        this.setModel(new DefaultComboBoxModel<>(this.choices));
        this.setSelectedIndex(3); // init value: 100%
        this.addActionListener(this);
        this.setVisible(true);
        this.setEditable(true);
        this.setUI(new BasicComboBoxUI());
        this.setFont(this.getFont().deriveFont((float)(this.getFont().getSize2D()*0.9)));
        JTextField text = (JTextField) this.getEditor().getEditorComponent();
        text.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        text.setColumns(3);
        this.setMaximumSize(this.getPreferredSize());

        this.diagramScene = scene;
        scene.getZoomChangedEvent().addListener(this);
    }

    private void setZoomLevel(int zoomLevel) {
        this.setSelectedItem(zoomLevel + "%");
        diagramScene.setZoomLevel(zoomLevel);
    }

    @Override
    public void changed(DiagramViewer diagramViewer) {
        this.setSelectedItem(diagramViewer.getZoomLevel() + "%");
    }
}
