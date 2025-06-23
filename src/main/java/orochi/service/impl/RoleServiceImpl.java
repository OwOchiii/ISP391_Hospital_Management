// src/main/java/orochi/service/impl/RoleServiceImpl.java
package orochi.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import orochi.service.RoleService;
import orochi.repository.RoleRepository;
import orochi.model.Role;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

//    @Override
//    public Role findById(Integer id) { return roleRepository.findById(id).orElse(null); }
}
