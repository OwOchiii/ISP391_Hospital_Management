package orochi.dto;

public class DoctorDTO {
    private Integer doctorId;
    private String fullName;
    private Integer specId;

    public DoctorDTO(Integer doctorId, String fullName, Integer specId) {
        this.doctorId = doctorId;
        this.fullName = fullName;
        this.specId = specId;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getSpecId() {
        return specId;
    }

    public void setSpecId(Integer specId) {
        this.specId = specId;
    }
}