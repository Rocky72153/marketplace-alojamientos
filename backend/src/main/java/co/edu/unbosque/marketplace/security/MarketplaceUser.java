package co.edu.unbosque.marketplace.security;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "marketplace_users")
public class MarketplaceUser implements UserDetails {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_state", nullable = false, length = 20)
    private AccountState accountState;

    @Column(length = 40)
    private String phone;

    @Column(name = "photo_reference", length = 1000)
    private String photoReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MarketplaceUser() {
    }

    public MarketplaceUser(String name, String email, String passwordHash, AccountRole role) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.accountState = AccountState.ACTIVE;
    }

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public AccountRole getRole() {
        return role;
    }

    public AccountState getAccountState() {
        return accountState;
    }

    public String getPhone() {
        return phone;
    }

    public String getPhotoReference() {
        return photoReference;
    }

    public void updateProfile(String name, String phone, String photoReference) {
        if (name != null) {
            this.name = name.trim();
        }
        if (phone != null) {
            this.phone = phone.trim();
        }
        if (photoReference != null) {
            this.photoReference = photoReference.trim();
        }
    }

    public void deactivate() {
        this.accountState = AccountState.DEACTIVATED;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Set.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountState != AccountState.RESTRICTED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountState == AccountState.ACTIVE;
    }
}
