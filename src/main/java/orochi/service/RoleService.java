// src/main/java/orochi/service/RoleService.java
package orochi.service;

import orochi.model.Role;

import java.util.List;

public interface RoleService {
    List<Role> getAllRoles();
    // (tuỳ chọn) Role findById(Integer id);
}
