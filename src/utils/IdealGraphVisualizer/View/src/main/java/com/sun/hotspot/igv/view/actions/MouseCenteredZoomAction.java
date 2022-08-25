/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package com.sun.hotspot.igv.view.actions;

import com.sun.hotspot.igv.view.DiagramScene;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.animator.AnimatorEvent;
import org.netbeans.api.visual.animator.AnimatorListener;
import org.netbeans.api.visual.animator.SceneAnimator;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.modules.visual.action.WheelPanAction;
import org.openide.util.Utilities;

import java.awt.*;
import java.awt.event.KeyEvent;


public class MouseCenteredZoomAction extends WidgetAction.Adapter {

    private static WidgetAction wheelPanAction = ActionFactory.createWheelPanAction();
    private double zoomMultiplier;
    private int modifiers;
    private DiagramScene scene;
    private volatile Point center;

    public MouseCenteredZoomAction(double zoomMultiplier, DiagramScene scene) {
        this.zoomMultiplier = zoomMultiplier;
        this.modifiers = Utilities.isMac() ? KeyEvent.META_MASK : KeyEvent.CTRL_MASK;
        this.scene = scene;

        scene.getSceneAnimator().getZoomAnimator().addAnimatorListener(new AnimatorListener() {
            private volatile Rectangle viewBounds;
            private volatile Point center;
            private volatile Point mouseLocation;

            @Override
            public void animatorStarted(AnimatorEvent animatorEvent) {}

            @Override
            public void animatorReset(AnimatorEvent animatorEvent) {}

            @Override
            public void animatorFinished(AnimatorEvent animatorEvent) {}

            @Override
            public void animatorPreTick(AnimatorEvent animatorEvent) {
                this.viewBounds = MouseCenteredZoomAction.this.scene.getView().getVisibleRect();
                this.center = MouseCenteredZoomAction.this.center;
                this.mouseLocation = MouseCenteredZoomAction.this.scene.convertSceneToView(this.center);

            }

            @Override
            public void animatorPostTick(AnimatorEvent animatorEvent) {
                Point center = MouseCenteredZoomAction.this.scene.convertSceneToView(this.center);
                Rectangle zoomRect = new Rectangle (
                        center.x - (mouseLocation.x - viewBounds.x),
                        center.y - (mouseLocation.y - viewBounds.y),
                        viewBounds.width,
                        viewBounds.height
                );
                MouseCenteredZoomAction.this.scene.getView().scrollRectToVisible(zoomRect);
            }
        });
    }

    @Override
    public State mouseWheelMoved(Widget widget, WidgetMouseWheelEvent event) {
        Scene scene = widget.getScene();
        if ((event.getModifiers() & modifiers) != modifiers) {
            // If modifier key is not pressed, use wheel for panning
            return wheelPanAction.mouseWheelMoved(widget, event);
        }
        this.center =  widget.convertLocalToScene(event.getPoint());

        SceneAnimator animator = scene.getSceneAnimator();
        int n = event.getWheelRotation();
        synchronized (animator) {
            double zoom = animator.isAnimatingZoomFactor() ? animator.getTargetZoomFactor() : scene.getZoomFactor();
            if (n > 0) {
                zoom /= zoomMultiplier;
            } else if (n < 0) {
                zoom *= zoomMultiplier;
            }
            if (zoom < this.scene.getZoomMinFactor()) {
                zoom = this.scene.getZoomMinFactor();
            } else if (zoom > this.scene.getZoomMaxFactor()) {
                zoom = this.scene.getZoomMaxFactor();
            }
            animator.animateZoomFactor(zoom);
        }

        return WidgetAction.State.CONSUMED;
    }
}
