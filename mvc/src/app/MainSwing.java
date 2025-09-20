package app;

import view.swing.MainFrame;

public class MainSwing {
    public static void main(String[] args) throws Exception {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                new MainFrame().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
