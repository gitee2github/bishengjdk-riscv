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

import com.sun.hotspot.igv.view.DiagramScene;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Utilities;

import java.awt.*;
import java.awt.event.KeyEvent;


public class MouseCenteredZoomAction extends WidgetAction.Adapter {

    private static WidgetAction wheelPanAction = ActionFactory.createWheelPanAction();
    private static int modifiers = Utilities.isMac() ? KeyEvent.META_MASK : KeyEvent.CTRL_MASK;
    private DiagramScene scene;

    public MouseCenteredZoomAction(DiagramScene scene) {
        this.scene = scene;
    }

    @Override
    public State mouseWheelMoved(Widget widget, WidgetMouseWheelEvent event) {
        Scene scene = widget.getScene();
        if ((event.getModifiers() & modifiers) != modifiers) {
            // If modifier key is not pressed, use wheel for panning
            return wheelPanAction.mouseWheelMoved(widget, event);
        }

        Point mouseLocation = widget.convertLocalToScene(event.getPoint());
        int n = event.getWheelRotation();
        if (n > 0) {
            this.scene.zoomOut(mouseLocation);
        } else if (n < 0) {
            this.scene.zoomIn(mouseLocation);
        }
        return State.CONSUMED;
    }
}
