package id.ac.sekolah.manajemenkelas.academic.model;

public class Subject {

    private Long id;
    private String code;
    private String name;
    private String description;
    private boolean active;

    public Subject(
            Long id,
            String code,
            String name,
            String description,
            boolean active
    ) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}