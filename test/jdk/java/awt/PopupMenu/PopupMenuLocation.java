/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 */

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

/**
 * @test
 * @key headful
 * @bug 8160270
 * @run main/timeout=300 PopupMenuLocation
 */
public final class PopupMenuLocation {

    public static final String TEXT =
            "Long-long-long-long-long-long-long text in the item-";
    private static final int SIZE = 350;
    private static Robot robot;
    private static volatile CountDownLatch actionEventReceivedLatch;

    public static void main(final String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(200);
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] sds = ge.getScreenDevices();
        for (GraphicsDevice sd : sds) {
            GraphicsConfiguration gc = sd.getDefaultConfiguration();
            Rectangle bounds = gc.getBounds();
            Point point = new Point(bounds.x, bounds.y);
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            while (point.y < bounds.y + bounds.height - insets.bottom - SIZE) {
                while (point.x <
                       bounds.x + bounds.width - insets.right - SIZE) {
                    test(point);
                    point.translate(bounds.width / 5, 0);
                }
                point.setLocation(bounds.x, point.y + bounds.height / 5);
            }
        }
    }

    private static void test(final Point tmp) throws Exception {
        actionEventReceivedLatch = new CountDownLatch(1);
        PopupMenu pm = new PopupMenu();
        IntStream.rangeClosed(1, 6).forEach(i -> pm.add(TEXT + i));
        pm.addActionListener(e -> actionEventReceivedLatch.countDown());
        Frame frame = new Frame();
        try {
            frame.setAlwaysOnTop(true);
            frame.setLayout(new FlowLayout());
            frame.add(pm);
            frame.pack();
            frame.setSize(SIZE, SIZE);
            frame.setVisible(true);
            frame.setLocation(tmp.x, tmp.y);
            frame.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    show(e);
                }

                public void mouseReleased(MouseEvent e) {
                    show(e);
                }

                private void show(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        pm.show(frame, 0, 50);
                    }
                }
            });
            openPopup(frame);
        } finally {
            frame.dispose();
        }
    }

    private static void openPopup(final Frame frame) throws Exception {
        robot.waitForIdle();
        Point pt = frame.getLocationOnScreen();
        robot.mouseMove(pt.x + frame.getWidth() / 2, pt.y + 50);
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        int x = pt.x + frame.getWidth() / 2;
        int y = pt.y + 130;
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
        if (!actionEventReceivedLatch.await(5, TimeUnit.SECONDS)) {
            captureScreen();
            throw new RuntimeException(
                    "Waited too long, but haven't received " +
                    "the PopupMenu ActionEvent yet");
        }
    }

    private static void captureScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        try {
            ImageIO.write(robot.createScreenCapture(
                    new Rectangle(0, 0, screenSize.width, screenSize.height)),
                          "png", new File("screen1.png"));
        } catch (IOException ignore) {
        }
    }

}
