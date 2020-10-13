package com.polygloat.controllers;

import com.polygloat.dtos.request.PermissionEditDto;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.Permission;
import com.polygloat.model.Repository;
import com.polygloat.model.UserAccount;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.testng.annotations.Test;

import static com.polygloat.Assertions.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PermissionControllerTest extends SignedInControllerTest implements ITest {

    @Test()
    void editPermissionTest() throws Exception {
        Repository base = dbPopulator.createBase(generateUniqueString());
        UserAccount user = dbPopulator.createUser(generateUniqueString());
        permissionService.grantFullAccessToRepo(user, base);

        entityManager.refresh(user);
        Permission permission = user.getPermissions().stream().findFirst().orElseThrow(NotFoundException::new);
        PermissionEditDto dto = PermissionEditDto.builder().permissionId(permission.getId()).type(Permission.RepositoryPermissionType.EDIT).build();
        performPost("/api/permission/edit", dto).andExpect(status().isOk()).andReturn();
        assertThat(permissionService.findById(permission.getId()).get().getType()).isEqualTo(Permission.RepositoryPermissionType.EDIT);
    }


}
