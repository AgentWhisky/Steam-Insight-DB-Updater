import com.formdev.flatlaf.FlatDarculaLaf;
import ui.Menu;

/**
 * Class - Used for running Steam DB Updater
 */
public class Updater {
    public static void main(String[] args) {
        FlatDarculaLaf.setup(); // Set FlatDarcula Look and Feel
        new Menu();
    }
}
