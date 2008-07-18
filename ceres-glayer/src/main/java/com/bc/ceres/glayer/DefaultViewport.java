package com.bc.ceres.glayer;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class DefaultViewport implements Viewport {
    private static final Point2D.Double V0 = new Point2D.Double(0, 0);

    private final AffineTransform viewToModelTransform;
    private final AffineTransform modelToViewTransform;
    private double currentRotation;
    private Rectangle2D modelArea;

    public DefaultViewport() {
        this.viewToModelTransform = new AffineTransform();
        this.modelToViewTransform = new AffineTransform();
    }

    /**
     * @return The affine transformation from view to model coordinates.
     */
    public AffineTransform getViewToModelTransform() {
        return new AffineTransform(viewToModelTransform);
    }

    /**
     * @return The affine transformation from model to view coordinates.
     */
    public AffineTransform getModelToViewTransform() {
        return new AffineTransform(modelToViewTransform);
    }

    public Rectangle2D getModelArea() {
        return modelArea;
    }

    public void setModelArea(Rectangle2D modelArea) {
        this.modelArea = modelArea;
    }

    /**
     * @return The coordinate in model CS which corresponds to origin at (0,0) in the view CS.
     */
    public Point2D getModelOffset() {
        return viewToModelTransform.transform(V0, null);
    }

    public double getModelOffsetX() {
        return getModelOffset().getX();
    }

    public double getModelOffsetY() {
        return getModelOffset().getY();
    }

    public void setModelOffset(double mpX, double mpY) {
        final Point2D p = getModelOffset();
        viewToModelTransform.translate(mpX-p.getX(), mpY-p.getY());
    }

    /**
     * The size of a view pixel in model coordinates.
     * @return size of a view pixel in model coordinates.
     */
    public double getModelScale() {
        return getScale(viewToModelTransform);
    }

    /**
     * Sets the model-to-view scaling factor relative to a given center point in view coordinates.
     *
     * @param modelScale the new size of a view pixel in model coordinates
     * @param viewCenter      the center of the zoom in the view CS
     */
    public void setModelScale(double modelScale, Point2D viewCenter) {
        final AffineTransform t = viewToModelTransform;
        final double m00 = t.getScaleX();
        final double m10 = t.getShearY();
        final double m01 = t.getShearX();
        final double m11 = t.getScaleY();
        final double sx = Math.sqrt(m00 * m00 + m10 * m10);
        final double sy = Math.sqrt(m01 * m01 + m11 * m11);
        t.translate(viewCenter.getX(), viewCenter.getY());
        t.scale(modelScale / sx, modelScale / sy);  // preserves signum & rotation of m00, m01, m10 and m11
        t.translate(-viewCenter.getX(), -viewCenter.getY());
        updateInverse();
    }

    /**
     * The rotation of the  of a view pixel in model coordinates.
     * @return size of a view pixel in model coordinates.
     */
    public double getModelRotation() {
        return currentRotation;
    }

    /**
     * Sets the model-to-view scaling factor relative to a given center point in view coordinates.
     *
     * @param theta  the new rotaton angle in radians
     * @param viewCenter      the center of the zoom in the view CS
     */
    public void setModelRotation(double theta, Point2D viewCenter) {
        final AffineTransform t = viewToModelTransform;
        t.translate(viewCenter.getX(), viewCenter.getY());
        t.rotate(theta - currentRotation);
        t.translate(-viewCenter.getX(), -viewCenter.getY());
        updateInverse();
        currentRotation = theta;
    }

    /**
     * Moves the model CS by translating it into the opposite direction of the given
     * vector in model coordinates.
     *
     * @param viewDelta the 'pan' vector in model coordinates
     */
    public void move(Point2D viewDelta) {
        viewToModelTransform.translate(-viewDelta.getX(), -viewDelta.getY());
        updateInverse();
    }

    // todo - provide a rotate() method here as well

    private void updateInverse() {
        try {
            modelToViewTransform.setTransform(viewToModelTransform.createInverse());
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "[viewToModelTransform=" + viewToModelTransform + "]";
    }

    public static double getScale(AffineTransform t) {
        final double m00 = t.getScaleX();
        final double m10 = t.getShearY();
        return m10 == 0.0 ? Math.abs(m00) : Math.sqrt(m00 * m00 + m10 * m10);
    }
}