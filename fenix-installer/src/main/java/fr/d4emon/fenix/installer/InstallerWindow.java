package fr.d4emon.fenix.installer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import javax.imageio.ImageIO;

/**
 * The window someone sees when they double-click the installer.
 *
 * <p>One screen, one button, and nothing to type. Installing a mod loader is
 * something people do once, often before they have any idea what a loader is,
 * and every field left open is a chance to fill it in wrongly.
 *
 * <p>The version was a text box, and it was the last thing that could go wrong:
 * a name that did not match a folder produced an error the player then had to
 * go and check. It offers what is actually installed instead — which is the
 * same condition the install needs, so anything listed will work.
 */
final class InstallerWindow {

    private static final Color EMBER = new Color(0xC2, 0x41, 0x0C);
    private static final Color ERROR = new Color(0xB3, 0x26, 0x1E);
    private static final Color SUCCESS = new Color(0x1E, 0x7A, 0x3C);
    private static final Color MUTED = new Color(0x60, 0x60, 0x60);

    private final String fenixVersion;
    private final List<String> supported;
    private final Supplier<List<Installer.Library>> payload;

    private final JTextField directory = new JTextField();
    private final JComboBox<String> minecraft = new JComboBox<>();
    private final JComboBox<String> fenix = new JComboBox<>();
    private final JLabel status = new JLabel(" ");
    private final JButton install = new JButton("Install");

    private InstallerWindow(String fenixVersion, List<String> supported,
                            Supplier<List<Installer.Library>> payload) {
        this.fenixVersion = fenixVersion;
        this.supported = supported;
        this.payload = payload;
    }

    /**
     * Opens the window.
     *
     * @param fenixVersion     the version this installer carries
     * @param supported        the game versions it carries it for
     * @param defaultDirectory where Minecraft usually lives on this machine
     * @param payload          reads the jars to install, when asked
     */
    static void open(String fenixVersion, List<String> supported, Path defaultDirectory,
                     Supplier<List<Installer.Library>> payload) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // The cross-platform look is not worth failing an install over.
            }
            new InstallerWindow(fenixVersion, supported, payload).show(defaultDirectory);
        });
    }

    private void show(Path defaultDirectory) {
        directory.setText(defaultDirectory.toString());

        JFrame frame = new JFrame("Fenix Installer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        icon().ifPresent(image -> frame.setIconImage(image));
        frame.setContentPane(content());
        frame.pack();
        frame.setMinimumSize(new Dimension(540, frame.getHeight()));
        frame.setLocationRelativeTo(null);

        install.addActionListener(event -> installNow());
        minecraft.addActionListener(event -> refreshFenixVersions());
        // Typing a path by hand is rare but allowed, and the list has to follow
        // it: a folder with different versions in it silently offering the old
        // folder's would be worse than no list at all.
        directory.addActionListener(event -> refreshVersions());

        refreshVersions();
        frame.setVisible(true);
    }

    private JPanel content() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));
        panel.add(heading(), BorderLayout.NORTH);
        panel.add(fields(), BorderLayout.CENTER);
        panel.add(actions(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel heading() {
        JLabel title = new JLabel("Fenix " + fenixVersion);
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize() + 8f));
        title.setForeground(EMBER);

        JLabel subtitle = new JLabel("Adds a Fenix profile to the Minecraft Launcher.");
        subtitle.setForeground(MUTED);

        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        words.add(title);
        words.add(Box.createVerticalStrut(3));
        words.add(subtitle);

        JPanel heading = new JPanel(new BorderLayout(14, 0));
        icon().ifPresent(image -> heading.add(
                new JLabel(new ImageIcon(image.getScaledInstance(44, 44, Image.SCALE_SMOOTH))),
                BorderLayout.WEST));
        heading.add(words, BorderLayout.CENTER);
        return heading;
    }

    private JPanel fields() {
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints label = new GridBagConstraints();
        label.gridx = 0;
        label.anchor = GridBagConstraints.LINE_START;
        label.insets = new Insets(0, 0, 4, 0);
        label.gridwidth = 2;

        GridBagConstraints field = new GridBagConstraints();
        field.gridx = 0;
        field.fill = GridBagConstraints.HORIZONTAL;
        field.weightx = 1;
        field.insets = new Insets(0, 0, 14, 0);

        JButton browse = new JButton("Browse…");
        browse.addActionListener(event -> chooseDirectory());

        int row = 0;
        row = add(fields, label, field, row, "Minecraft folder", directory, browse);
        row = add(fields, label, field, row, "Game version", minecraft, null);
        add(fields, label, field, row, "Fenix version", fenix, null);
        return fields;
    }

    private static int add(JPanel panel, GridBagConstraints label, GridBagConstraints field,
                           int row, String text, Component control, Component trailing) {
        label.gridy = row;
        panel.add(new JLabel(text), label);

        field.gridy = row + 1;
        field.gridwidth = trailing == null ? 2 : 1;
        panel.add(control, field);

        if (trailing != null) {
            GridBagConstraints beside = new GridBagConstraints();
            beside.gridx = 1;
            beside.gridy = row + 1;
            beside.insets = new Insets(0, 8, 14, 0);
            panel.add(trailing, beside);
        }
        return row + 2;
    }

    private JPanel actions() {
        install.setFont(install.getFont().deriveFont(Font.BOLD));
        JPanel actions = new JPanel(new BorderLayout(12, 0));
        actions.add(status, BorderLayout.CENTER);
        actions.add(install, BorderLayout.EAST);
        return actions;
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser(directory.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Where Minecraft is installed");
        if (chooser.showOpenDialog(install) == JFileChooser.APPROVE_OPTION) {
            directory.setText(chooser.getSelectedFile().getAbsolutePath());
            refreshVersions();
        }
    }

    /** Rebuilds both lists from whatever is in the chosen folder. */
    private void refreshVersions() {
        Path target = Path.of(directory.getText().trim());
        List<String> installable = Files.isDirectory(target)
                ? Versions.installable(target, supported)
                : List.of();

        minecraft.setModel(new DefaultComboBoxModel<>(installable.toArray(String[]::new)));
        refreshFenixVersions();

        boolean ready = !installable.isEmpty();
        install.setEnabled(ready);
        minecraft.setEnabled(ready);
        fenix.setEnabled(ready);

        if (!Files.isDirectory(target)) {
            report(ERROR, "That folder does not exist.");
        } else if (installable.isEmpty()) {
            // The most likely reason by far, and the player can act on it.
            report(MUTED, "Install Minecraft " + String.join(" or ", supported)
                    + " from the launcher and run it once.");
        } else {
            report(MUTED, " ");
        }
    }

    private void refreshFenixVersions() {
        Object selected = minecraft.getSelectedItem();
        List<String> versions = selected == null
                ? List.of()
                : Versions.forMinecraft(selected.toString(), fenixVersion, supported);
        fenix.setModel(new DefaultComboBoxModel<>(versions.toArray(String[]::new)));
    }

    private void installNow() {
        Object game = minecraft.getSelectedItem();
        if (game == null) {
            return;
        }
        setBusy(true);
        report(MUTED, "Installing…");

        // Off the event thread: the install writes files and unpacks jars, and
        // doing that here would freeze the window it is reporting into.
        new SwingWorker<Installer.Report, Void>() {

            @Override
            protected Installer.Report doInBackground() {
                return Installer.install(Path.of(directory.getText().trim()), game.toString(),
                        fenixVersion, payload.get());
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
        fenix.setEnabled(!busy);
    }

    private void report(Color colour, String message) {
        status.setForeground(colour);
        status.setText(message);
    }

    /** {@return the Fenix mark, if it travelled with us} */
    private static java.util.Optional<Image> icon() {
        URL resource = InstallerWindow.class.getResource("/fenix-logo.png");
        if (resource == null) {
            return java.util.Optional.empty();
        }
        try (InputStream in = resource.openStream()) {
            return java.util.Optional.ofNullable(ImageIO.read(in));
        } catch (IOException e) {
            // A missing icon is not worth refusing to install over.
            return java.util.Optional.empty();
        }
    }
}
