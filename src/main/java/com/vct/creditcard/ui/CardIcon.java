package com.vct.creditcard.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** A credit card drawn with Java2D, used as the application logo (no image files needed). */
final class CardIcon implements Icon {

    private static final Color DARK_BLUE = new Color(0x0D, 0x47, 0xA1);
    private static final Color LIGHT_BLUE = new Color(0x42, 0xA5, 0xF5);
    private static final Color CHIP = new Color(0xFF, 0xD5, 0x4F);

    private final int width;
    private final int height;

    CardIcon(int width) {
        this.width = width;
        this.height = Math.round(width * 0.63f);
    }

    @Override
    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.translate(x, y);
            float arc = width * 0.08f;

            g.setPaint(new GradientPaint(0, 0, DARK_BLUE, width, height, LIGHT_BLUE));
            g.fill(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, arc, arc));

            g.setColor(CHIP);
            g.fill(new RoundRectangle2D.Float(width * 0.10f, height * 0.33f, width * 0.16f, height * 0.22f, 6, 6));
            g.setColor(CHIP.darker());
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(width * 0.10f, height * 0.33f, width * 0.16f, height * 0.22f, 6, 6));

            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.round(height * 0.18f)));
            g.drawString("VCT", width * 0.10f, height * 0.24f);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.round(height * 0.11f)));
            g.drawString("4000 1234 5678 9010", width * 0.10f, height * 0.75f);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(height * 0.08f)));
            g.drawString("VALID THRU 11/30", width * 0.10f, height * 0.90f);

            g.setColor(new Color(255, 255, 255, 120));
            g.fillOval(Math.round(width * 0.70f), Math.round(height * 0.10f), Math.round(height * 0.22f), Math.round(height * 0.22f));
            g.setColor(new Color(255, 255, 255, 180));
            g.fillOval(Math.round(width * 0.79f), Math.round(height * 0.10f), Math.round(height * 0.22f), Math.round(height * 0.22f));
        } finally {
            g.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}
