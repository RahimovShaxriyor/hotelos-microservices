package com.hotelos.common.dto.guest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateGuestRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 64, message = "First name must not exceed 64 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 64, message = "Last name must not exceed 64 characters")
    private String lastName;

    @Email(message = "Invalid email format")
    @Size(max = 128, message = "Email must not exceed 128 characters")
    private String email;

    @Pattern(regexp = "^\\+?[0-9\\s\\-().]{7,32}$", message = "Invalid phone number format")
    private String phone;

    private Boolean vipStatus;

    @Size(max = 512, message = "Notes must not exceed 512 characters")
    private String notes;

    public UpdateGuestRequest() {}

    public UpdateGuestRequest(String firstName, String lastName, String email, String phone, Boolean vipStatus, String notes) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.vipStatus = vipStatus;
        this.notes = notes;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Boolean getVipStatus() { return vipStatus; }
    public void setVipStatus(Boolean vipStatus) { this.vipStatus = vipStatus; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
