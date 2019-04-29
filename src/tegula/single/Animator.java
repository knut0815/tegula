/*
 * Animator.java Copyright (C) 2019. Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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

package tegula.single;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * Translate or rotate animation
 * Daniel Huson, 4.2019
 */
public class Animator {
    private final Timeline timeline;
    private BooleanProperty playing = new SimpleBooleanProperty(false);
    private BooleanProperty paused = new SimpleBooleanProperty(false);

    private double dx;
    private double dy;

    private double angle;
    private Point3D rotationalAxis;

    private final SingleTilingPane singleTilingPane;

    /**
     * setup animation
     *
     * @param singleTilingPane
     */
    public Animator(SingleTilingPane singleTilingPane) {
        this.singleTilingPane = singleTilingPane;
        timeline = new Timeline(Timeline.INDEFINITE);
        timeline.setCycleCount(10000);
    }

    /**
     * set a rotational animation
     *
     * @param rotationalAxis
     * @param angle
     * @param millis
     */
    public void set(final Point3D rotationalAxis, double angle, final long millis) {
        dx = dy = 0;
        double factor = 100.0 / millis;
        this.angle = angle;
        this.rotationalAxis = rotationalAxis;
        timeline.stop();
        final KeyFrame keyFrame = new KeyFrame(Duration.millis(10), (e) -> {
            final Rotate rotate = new Rotate(factor * angle, rotationalAxis);
            singleTilingPane.setWorldRotate(rotate.createConcatenation(singleTilingPane.getWorldRotate()));
        });
        timeline.getKeyFrames().setAll(keyFrame);
        timeline.playFromStart();
    }

    /**
     * set a translational animation
     *
     * @param dx0
     * @param dy0
     * @param millis
     */
    public void set(final double dx0, final double dy0, final long millis) {
        angle=0;
        double factor = 100.0 / millis;
        this.dx = factor * dx0;
        this.dy = factor * dy0;
        timeline.stop();
        final KeyFrame keyFrame = new KeyFrame(Duration.millis(10), (e) -> {
            singleTilingPane.translateTiling(dx, dy);
        });
        timeline.getKeyFrames().setAll(keyFrame);
        timeline.playFromStart();
    }

    public void play() {
        if (rotationalAxis != null && angle != 0 || dx !=0 || dy!=0) {
            timeline.play();
            playing.set(true);
            paused.set(false);
        }
    }

    public void pause() {
        timeline.pause();
        playing.set(false);
        paused.set(true);
    }

    public void stop() {
        timeline.stop();
        playing.set(false);
        paused.set(false);
    }

    public boolean isPlaying() {
        return playing.get();
    }

    public ReadOnlyBooleanProperty playingProperty() {
        return playing;
    }

    public boolean isPaused() {
        return paused.get();
    }

    public ReadOnlyBooleanProperty pausedProperty() {
        return paused;
    }
}
