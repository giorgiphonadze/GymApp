package com.gymcrm.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public abstract class User {
    private Long userId;
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private boolean isActive;

    protected User(String firstName, String lastName){
        this.firstName = firstName;
        this.lastName = lastName;
        this.isActive = true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj){
            return true;
        }
        if (!(obj instanceof User user)){
            return false;
        }
        return Objects.equals(userId,user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}
