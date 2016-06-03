package semrau.brian.gaslawsdemo.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import semrau.brian.gaslawsdemo.GasLawsDemo;

public class DesktopLauncher {

    public static void main(String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.width = 720;
        config.height = 720;
        new LwjglApplication(new GasLawsDemo(), config);
    }

}
