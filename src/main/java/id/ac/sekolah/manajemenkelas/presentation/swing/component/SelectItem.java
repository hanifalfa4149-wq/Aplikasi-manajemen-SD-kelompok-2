package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.util.Objects;

/**
 * Item pembungkus dropdown (JComboBox) agar ID/objek domain tampil dengan label ramah pengguna.
 */
public final class SelectItem<T> {

    private final T value;
    private final String label;

    public SelectItem(T value, String label) {
        this.value = value;
        this.label = label == null ? "-" : label;
    }

    public T getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SelectItem<?> other)) {
            return false;
        }
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}

