package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Pembungkus input formulir yang seragam dengan label, indikator wajib (*),
 * teks bantuan, dan pesan validasi di bawah field.
 */
public class FormField extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel requiredLabel = new JLabel(" *");
    private final JLabel messageLabel = new JLabel();
    private final JComponent inputComponent;
    private String hintText;

    public FormField(String label, JComponent inputComponent, boolean required) {
        this(label, inputComponent, required, null);
    }

    public FormField(String label, JComponent inputComponent, boolean required, String hintText) {
        this.inputComponent = inputComponent;
        this.hintText = hintText;

        setOpaque(false);
        setLayout(new BorderLayout(0, 5));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setOpaque(false);
        titleLabel.setText(label);
        titleLabel.setFont(BotanicalTheme.FONT_SMALL_BOLD);
        titleLabel.setForeground(BotanicalTheme.TEXT_PRIMARY);
        header.add(titleLabel);

        requiredLabel.setFont(BotanicalTheme.FONT_SMALL_BOLD);
        requiredLabel.setForeground(BotanicalTheme.DANGER);
        requiredLabel.setVisible(required);
        header.add(requiredLabel);
        add(header, BorderLayout.NORTH);

        styleInputComponent(inputComponent);
        add(inputComponent, BorderLayout.CENTER);

        messageLabel.setFont(BotanicalTheme.FONT_SMALL);
        setError(null);
        add(messageLabel, BorderLayout.SOUTH);
    }

    public JComponent getInputComponent() {
        return inputComponent;
    }

    public void setError(String errorText) {
        if (errorText != null && !errorText.isBlank()) {
            messageLabel.setForeground(BotanicalTheme.DANGER);
            messageLabel.setText(errorText);
            messageLabel.setVisible(true);
            applyBorderState(true);
        } else if (hintText != null && !hintText.isBlank()) {
            messageLabel.setForeground(BotanicalTheme.TEXT_SECONDARY);
            messageLabel.setText(hintText);
            messageLabel.setVisible(true);
            applyBorderState(false);
        } else {
            messageLabel.setText(" ");
            messageLabel.setVisible(true);
            applyBorderState(false);
        }
    }

    public void clearError() {
        setError(null);
    }

    private void applyBorderState(boolean error) {
        Color borderColor = error ? BotanicalTheme.DANGER : BotanicalTheme.BORDER;
        if (inputComponent instanceof JTextField || inputComponent instanceof JPasswordField) {
            inputComponent.setBorder(createInputBorder(borderColor));
        } else if (inputComponent instanceof JComboBox<?>) {
            inputComponent.setBorder(BorderFactory.createLineBorder(borderColor, 1));
        } else if (inputComponent instanceof JScrollPane) {
            inputComponent.setBorder(BorderFactory.createLineBorder(borderColor, 1));
        }
    }

    public static void styleInputComponent(JComponent component) {
        if (component instanceof JTextField || component instanceof JPasswordField) {
            component.setFont(BotanicalTheme.FONT_BODY);
            component.setForeground(BotanicalTheme.TEXT_PRIMARY);
            component.setBackground(BotanicalTheme.WHITE);
            component.setBorder(createInputBorder(BotanicalTheme.BORDER));
            component.setPreferredSize(new Dimension(240, BotanicalTheme.INPUT_HEIGHT));
        } else if (component instanceof JComboBox<?> combo) {
            combo.setFont(BotanicalTheme.FONT_BODY);
            combo.setForeground(BotanicalTheme.TEXT_PRIMARY);
            combo.setBackground(BotanicalTheme.WHITE);
            combo.setBorder(BorderFactory.createLineBorder(BotanicalTheme.BORDER, 1));
            combo.setPreferredSize(new Dimension(240, BotanicalTheme.INPUT_HEIGHT));
        } else if (component instanceof JScrollPane scroll) {
            scroll.setBorder(BorderFactory.createLineBorder(BotanicalTheme.BORDER, 1));
            scroll.getViewport().setBackground(BotanicalTheme.WHITE);
            Component view = scroll.getViewport().getView();
            if (view instanceof JTextArea area) {
                area.setFont(BotanicalTheme.FONT_BODY);
                area.setForeground(BotanicalTheme.TEXT_PRIMARY);
                area.setBackground(BotanicalTheme.WHITE);
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            }
        }
    }

    public static JScrollPane wrapTextArea(JTextArea area, int height) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(240, height));
        styleInputComponent(scroll);
        return scroll;
    }

    private static Border createInputBorder(Color borderColor) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }
}

