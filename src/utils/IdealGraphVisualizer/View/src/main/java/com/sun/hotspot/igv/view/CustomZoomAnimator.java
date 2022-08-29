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
package com.sun.hotspot.igv.view;

import java.awt.Point;
import java.awt.Rectangle;
import org.netbeans.api.visual.animator.Animator;
import org.netbeans.api.visual.animator.SceneAnimator;

public class CustomZoomAnimator extends Animator {
    private volatile double sourceZoom;
    private volatile double targetZoom;
    private volatile Point zoomCenter;

    public CustomZoomAnimator(SceneAnimator sceneAnimator) {
        super(sceneAnimator);
    }

    public synchronized void animateZoomFactor(double zoomFactor, Point zoomCenter) {
        this.targetZoom = zoomFactor;
        if (!this.isRunning()) {
            this.zoomCenter = zoomCenter;
            this.sourceZoom = this.getScene().getZoomFactor();
            this.start();
        }
    }

    public synchronized double getTargetZoom() {
        if (this.isRunning()) {
            return this.targetZoom;
        } else {
            return this.getScene().getZoomFactor();
        }
    }

    public void tick(double progress) {
        Rectangle oldVisibleRect = this.getScene().getView().getVisibleRect();
        if (this.zoomCenter == null) {
            this.zoomCenter = new Point(oldVisibleRect.x + oldVisibleRect.width / 2, oldVisibleRect.y + oldVisibleRect.height / 2);
            this.zoomCenter = this.getScene().convertViewToScene(this.zoomCenter);
        }
        Point oldViewCenter = this.getScene().convertSceneToView(this.zoomCenter);

        double newZoom = progress >= 1.0 ? this.targetZoom : this.sourceZoom + progress * (this.targetZoom - this.sourceZoom);
        this.getScene().setZoomFactor(newZoom);
        this.getScene().validate();

        Point newViewCenter = this.getScene().convertSceneToView(this.zoomCenter);
        Rectangle newVisibleRect = new Rectangle (
                newViewCenter.x - (oldViewCenter.x - oldVisibleRect.x),
                newViewCenter.y - (oldViewCenter.y - oldVisibleRect.y),
                oldVisibleRect.width,
                oldVisibleRect.height
        );
        this.getScene().getView().scrollRectToVisible(newVisibleRect);
    }
}
