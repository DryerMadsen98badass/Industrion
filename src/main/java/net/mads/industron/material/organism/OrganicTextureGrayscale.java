package net.mads.industron.material.organism;

import java.awt.image.BufferedImage;
import java.util.Objects;

/** Rec.709 conversion, matching tools/grayscale_texture.py without resizing or changing alpha. */
public final class OrganicTextureGrayscale {
    private OrganicTextureGrayscale() {}
    public static BufferedImage convert(BufferedImage source,double shade) {
        Objects.requireNonNull(source);
        if(!Double.isFinite(shade) || shade<0 || shade>1)throw new IllegalArgumentException("Shade must be between 0 and 1");
        var gray=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++) {
            int pixel=source.getRGB(x,y),alpha=pixel>>>24;
            if(alpha==0)continue;
            int value=(int)Math.round((0.2126*((pixel>>>16)&255)+0.7152*((pixel>>>8)&255)+0.0722*(pixel&255))*shade);
            gray.setRGB(x,y,(alpha<<24)|(value<<16)|(value<<8)|value);
        }
        return gray;
    }
}
