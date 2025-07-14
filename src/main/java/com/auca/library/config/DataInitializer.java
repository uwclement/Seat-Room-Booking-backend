package com.auca.library.config;

import com.auca.library.model.Role;
import com.auca.library.model.User;
import com.auca.library.model.Location;
import com.auca.library.repository.RoleRepository;
import com.auca.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Initialize roles if they don't exist
        initializeRoles();
        
        // Create default admin if none exists
        createDefaultAdmin();
    }

    private void initializeRoles() {
        for (Role.ERole roleName : Role.ERole.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role(roleName);
                roleRepository.save(role);
                System.out.println("Created role: " + roleName);
            }
        }
    }

    private void createDefaultAdmin() {
        // Check if any admin exists
        if (userRepository.findAllAdmins().isEmpty()) {
            User admin = new User(
                "System Administrator",
                "admin@library.com",
                "ADMIN001",
                passwordEncoder.encode("Admin123!"),
                Location.GISHUSHU, 
                "+250788000000"
            );

            Set<Role> roles = new HashSet<>();
            Role adminRole = roleRepository.findByName(Role.ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            roles.add(adminRole);
            admin.setRoles(roles);
            admin.setMustChangePassword(true);

            userRepository.save(admin);
            System.out.println("Created default admin user: admin@library.com / Admin123!");
        }
    }
}