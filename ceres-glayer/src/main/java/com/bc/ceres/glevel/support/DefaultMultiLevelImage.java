package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelImage;

import javax.media.jai.RenderedImageAdapter;
import java.awt.image.RenderedImage;

/**
 * Adapts a JAI {@link javax.media.jai.PlanarImage PlanarImage} to the {@link com.bc.ceres.glevel.MultiLevelSource} interface.
 * The image data provided by this {@code PlanarImage} corresponds to the level zero image of the given
 * {@code MultiLevelSource}.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class DefaultMultiLevelImage extends RenderedImageAdapter implements MultiLevelImage {

    private final MultiLevelSource source;

    public DefaultMultiLevelImage(MultiLevelSource source) {
        super(source.getImage(0));
        this.source = source;
    }

    public MultiLevelSource getSource() {
        return source;
    }

    @Override
    public MultiLevelModel getModel() {
        return source.getModel();
    }

    @Override
    public RenderedImage getImage(int level) {
        return source.getImage(level);
    }

    @Override
    public void reset() {
        source.reset();
    }

    @Override
    public synchronized void dispose() {
        reset();
        super.dispose();
    }
}