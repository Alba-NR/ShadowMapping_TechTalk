package graphics.core.io;

import graphics.core.WindowManager;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL30.*;

public class ScreenshotMaker {
    private static String directory = "./screenshots/screenshot_";
    private static DateFormat dateFormat = new SimpleDateFormat("dd-mm-yyyy_hh-mm-ss");

    public static void takeScreenshot(){
        String strDate = dateFormat.format(Calendar.getInstance().getTime()); // get date & time as str

        int bpp = 4;
        int width = WindowManager.getScrWidth();
        int height = WindowManager.getScrHeight();

        glReadBuffer(GL_COLOR_ATTACHMENT0);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * bpp);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);


        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                int index = (i + width * (height - j - 1)) * bpp;
                int r = buffer.get(index + 0) & 0xFF;
                int g = buffer.get(index + 1) & 0xFF;
                int b = buffer.get(index + 2) & 0xFF;
                image.setRGB(i, j, 0xFF << 24 | r << 16 | g << 8 | b);
            }
        }
        try {
            ImageIO.write(image, "png", new File(directory + strDate + ".png"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write screenshot img output file.");
        }

    }
}
