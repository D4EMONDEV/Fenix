package fr.d4emon.fenix.installer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * The window someone sees when they double-click the installer.
 *
 * <p>Deliberately one screen with one button. Installing a mod loader is a
 * thing people do once, often before they have any idea what a loader is, and
 * every question asked here is a chance to answer it wrongly. The two fields
 * are filled in already; the usual visit is to press Install.
 */
final class InstallerWindow {

    private static final Color ERROR = new Color(0xB3, 0x26, 0x1E);
    private static final Color SUCCESS = new Color(0x1E, 0x7A, 0x3C);

    private final String fenixVersion;
    private final String minecraftVersion;
    private final Supplier<List<Installer.Library>> payload;

    private final JTextField directory = new JTextField();
    private final JTextField minecraft = new JTextField();
    private final JLabel status = new JLabel(" ");
    private final JButton install = new JButton("Install");

    private InstallerWindow(String fenixVersion, String minecraftVersion,
                            Supplier<List<Installer.Library>> payload) {
        this.fenixVersion = fenixVersion;
        this.minecraftVersion = minecraftVersion;
        this.payload = payload;
    }

    /**
     * Opens the window.
     *
     * @param fenixVersion     the version being installed
     * @param minecraftVersion the game version it was built for
     * @param defaultDirectory where Minecraft usually lives on this machine
     * @param payload          reads the jars to install, when asked
     */
    static void open(String fenixVersion, String minecraftVersion, Path defaultDirectory,
                     Supplier<List<Installer.Library>> payload) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // The cross-platform look is not worth failing an install over.
            }
            new InstallerWindow(fenixVersion, minecraftVersion, payload).show(defaultDirectory);
        });
    }

    private void show(Path defaultDirectory) {
        directory.setText(defaultDirectory.toString());
        minecraft.setText(minecraftVersion);

        JFrame frame = new JFrame("Fenix Installer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(content());
        frame.pack();
        frame.setMinimumSize(new Dimension(520, frame.getHeight()));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        install.addActionListener(event -> installNow());
    }

    private JPanel content() {
        JLabel title = new JLabel("Fenix " + fenixVersion);
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize() + 6f));

        JLabel subtitle = new JLabel("Adds a Fenix profile to the Minecraft Launcher.");
        subtitle.setForeground(new Color(0x55, 0x55, 0x55));

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);

        JPanel fields = new JPanel(new GridLayout(2, 1, 0, 8));
        fields.add(labelled("Minecraft folder", directory, browse()));
        fields.add(labelled("Game version", minecraft, null));

        JPanel actions = new JPanel(new BorderLayout(8, 0));
        actions.add(status, BorderLayout.CENTER);
        actions.add(install, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(fields, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JButton browse() {
        JButton button = new JButton("Browse…");
        button.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser(directory.getText());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Where Minecraft is installed");
            if (chooser.showOpenDialog(install) == JFileChooser.APPROVE_OPTION) {
                directory.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        return button;
    }

    private static JPanel labelled(String text, JTextField field, JButton trailing) {
        JPanel row = new JPanel(new BorderLayout(8, 2));
        row.add(new JLabel(text), BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        if (trailing != null) {
            row.add(trailing, BorderLayout.EAST);
        }
        return row;
    }

    private void installNow() {
        Path target = Path.of(directory.getText().trim());
        if (!Files.isDirectory(target)) {
            report(ERROR, "That folder does not exist.");
            return;
        }
        setBusy(true);
        report(Color.GRAY, "Installing…");

        // Off the event thread: the install writes files and unpacks jars, and
        // doing that on the EDT would freeze the window it is reporting into.
        new SwingWorker<Installer.Report, Void>() {

            @Override
            protected Installer.Report doInBackground() {
                return Installer.install(target, minecraft.getText().trim(), fenixVersion, payload.get());
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    get();
                    report(SUCCESS, "Done — pick the Fenix profile in the launcher.");
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    report(ERROR, cause.getMessage() == null ? cause.toString() : cause.getMessage());
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        install.setEnabled(!busy);
        directory.setEnabled(!busy);
        minecraft.setEnabled(!busy);
    }

    private void report(Color colour, String message) {
        status.setForeground(colour);
        status.setText(message);
    }
}
