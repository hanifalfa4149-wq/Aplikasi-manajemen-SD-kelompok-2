package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Field pencarian rounded dengan ikon kaca pembesar, placeholder, dan tombol hapus cepat.
 */
public class SearchField extends JPanel {

    private final JTextField textField;
    private final JButton clearButton;
    private String placeholder;
    private boolean focused = false;

    public SearchField(String placeholder) {
        this.placeholder = placeholder;
        setLayout(new BorderLayout(8, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 8));
        setPreferredSize(new Dimension(280, BotanicalTheme.INPUT_HEIGHT));

        JLabel searchIcon = new JLabel(BotanicalIcons.get(BotanicalIcons.Type.SEARCH, 16, BotanicalTheme.TEXT_SECONDARY));
        add(searchIcon, BorderLayout.WEST);

        textField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && SearchField.this.placeholder != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    try {
                        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        g2.setColor(BotanicalTheme.TEXT_SECONDARY);
                        g2.setFont(getFont());
                        int y = (getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
                        g2.drawString(SearchField.this.placeholder, 2, y);
                    } finally {
                        g2.dispose();
                    }
                }
            }
        };
        textField.setBorder(BorderFactory.createEmptyBorder());
        textField.setOpaque(false);
        textField.setFont(BotanicalTheme.FONT_BODY);
        textField.setForeground(BotanicalTheme.TEXT_PRIMARY);
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                focused = false;
                repaint();
            }
        });
        add(textField, BorderLayout.CENTER);

        clearButton = new JButton("×");
        clearButton.setFont(BotanicalTheme.FONT_SUBTITLE);
        clearButton.setForeground(BotanicalTheme.TEXT_SECONDARY);
        clearButton.setOpaque(false);
        clearButton.setContentAreaFilled(false);
        clearButton.setBorderPainted(false);
        clearButton.setFocusPainted(false);
        clearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearButton.setVisible(false);
        clearButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        clearButton.addActionListener(e -> {
            textField.setText("");
            textField.requestFocusInWindow();
        });
        add(clearButton, BorderLayout.EAST);

        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                clearButton.setVisible(!textField.getText().isEmpty());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                clearButton.setVisible(!textField.getText().isEmpty());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                clearButton.setVisible(!textField.getText().isEmpty());
            }
        });
    }

    public String getText() {
        return textField.getText().trim();
    }

    public void setText(String text) {
        textField.setText(text == null ? "" : text);
    }

    public void addSearchListener(Consumer<String> listener) {
        textField.addActionListener(e -> listener.accept(getText()));
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                listener.accept(getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                listener.accept(getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listener.accept(getText());
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(BotanicalTheme.WHITE);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, BotanicalTheme.RADIUS_CONTROL, BotanicalTheme.RADIUS_CONTROL));
            g2.setColor(focused ? BotanicalTheme.EVERGREEN : BotanicalTheme.BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, BotanicalTheme.RADIUS_CONTROL, BotanicalTheme.RADIUS_CONTROL));
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}

