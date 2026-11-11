package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Kontrol pemilihan tanggal (format YYYY-MM-DD) berbasis JSpinner + tombol cepat "Hari Ini".
 */
public class DateField extends JPanel {

    private final SpinnerDateModel model;
    private final JSpinner spinner;
    private final SecondaryButton todayButton;

    public DateField() {
        this(LocalDate.now());
    }

    public DateField(LocalDate initialDate) {
        setOpaque(false);
        setLayout(new BorderLayout(6, 0));
        setPreferredSize(new Dimension(216, BotanicalTheme.INPUT_HEIGHT));

        Date init = toDate(initialDate != null ? initialDate : LocalDate.now());
        this.model = new SpinnerDateModel(init, null, null, Calendar.DAY_OF_MONTH);
        this.spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
        spinner.setEditor(editor);
        spinner.setFont(BotanicalTheme.FONT_BODY);
        spinner.setBackground(BotanicalTheme.WHITE);
        spinner.setBorder(BorderFactory.createLineBorder(BotanicalTheme.BORDER, 1));
        editor.getTextField().setFont(BotanicalTheme.FONT_BODY);
        editor.getTextField().setForeground(BotanicalTheme.TEXT_PRIMARY);
        editor.getTextField().setBackground(BotanicalTheme.WHITE);
        editor.getTextField().setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        this.todayButton = new SecondaryButton("Hari Ini");
        this.todayButton.setPreferredSize(new Dimension(74, BotanicalTheme.INPUT_HEIGHT));
        this.todayButton.setMargin(new java.awt.Insets(4, 6, 4, 6));
        this.todayButton.addActionListener(e -> setLocalDate(LocalDate.now()));

        add(spinner, BorderLayout.CENTER);
        add(todayButton, BorderLayout.EAST);
    }

    public LocalDate getLocalDate() {
        Date value = model.getDate();
        if (value == null) {
            return null;
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public void setLocalDate(LocalDate date) {
        if (date != null) {
            model.setValue(toDate(date));
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        spinner.setEnabled(enabled);
        todayButton.setEnabled(enabled);
    }

    private static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}

