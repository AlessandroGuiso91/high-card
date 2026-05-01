package it.sara.demo.service.assembler;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.service.database.model.User;
import org.springframework.stereotype.Component;

/**
 * Maps a persistence-layer {@link User} into the layer-neutral {@link UserDTO}
 * carried by service results. Pure projection — no business logic.
 */
@Component
public class UserAssembler {

    public UserDTO toDTO(User user) {
        UserDTO returnValue = new UserDTO();
        returnValue.setEmail(user.getEmail());
        returnValue.setGuid(user.getGuid());
        returnValue.setFirstName(user.getFirstName());
        returnValue.setLastName(user.getLastName());
        returnValue.setPhoneNumber(user.getPhoneNumber());
        return returnValue;
    }
}
