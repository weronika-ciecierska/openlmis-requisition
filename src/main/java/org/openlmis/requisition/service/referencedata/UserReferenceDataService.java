package org.openlmis.requisition.service.referencedata;

import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.exception.AuthorizationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserReferenceDataService extends BaseReferenceDataService<UserDto> {

  @Override
  protected String getUrl() {
    return "/api/users/";
  }

  @Override
  protected Class<UserDto> getResultClass() {
    return UserDto.class;
  }

  @Override
  protected Class<UserDto[]> getArrayResultClass() {
    return UserDto[].class;
  }

  public Collection<UserDto> findUsers(Map<String, Object> parameters) {
    return findAll("search", parameters);
  }

  /**
   * This method retrieves a user with given name.
   *
   * @param name the name of user.
   * @return UserDto containing user's data, or null if such user was not found.
   */
  public UserDto findUser(String name) throws AuthorizationException {
    Map<String, Object> parameters = new HashMap<>();
    List<UserDto> users = new ArrayList<>(findAll("search", parameters));

    if (users.size() == 0) {
      throw new AuthorizationException("User with name: " + name + " not found.");
    }

    return users.get(0);
  }
}
